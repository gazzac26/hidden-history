package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine
import com.hiddenhistory.models.GrandCMaxTest
import com.hiddenhistory.models.HiddenHistoryReportEntity
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import com.hiddenhistory.repository.HiddenHistoryRepository
import com.hiddenhistory.ui.debug.DebugStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class FreeVehicleSearchViewModel : ViewModel() {

    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private val client =
        OkHttpClient()

    /*
     * =========================================================
     * EXISTING PRODUCTION HELPERS
     * =========================================================
     */

    private val hiddenHistoryRepository =
        HiddenHistoryRepository(
            SupabaseManager.client
        )

    private val stateHolder =
        VehicleSearchState()

    private val advertProcessor =
        VehicleSearchAdvertProcessor()

    private val officialLookup =
        FreeVehicleSearchOfficialLookup(
            client
        )

    private val reportBuilder =
        VehicleSearchReportBuilder()

    private val savedReportsHandler =
        VehicleSearchSavedReports(
            hiddenHistoryRepository,
            jsonParser
        )

    private val partsHandler =
        VehicleSearchParts(
            client
        )

    /*
     * =========================================================
     * ADVERT OFFICIAL CROSS-CHECK
     * =========================================================
     */

    private val advertOfficialCrossCheckEngine =
        AdvertOfficialCrossCheckEngine()

    private val _officialCrossCheck =
        MutableStateFlow<
            AdvertOfficialCrossCheckEngine.CrossCheckResult?
        >(null)

    val officialCrossCheck:
        StateFlow<
            AdvertOfficialCrossCheckEngine.CrossCheckResult?
        > =
        _officialCrossCheck.asStateFlow()

    /*
     * =========================================================
     * SELECTED MOT TEST
     * =========================================================
     */

    private val _selectedMotTest =
        MutableStateFlow<MotTest?>(null)

    val selectedMotTest:
        StateFlow<MotTest?> =
        _selectedMotTest.asStateFlow()

    fun selectMotTest(
        motTest: MotTest?
    ) {

        _selectedMotTest.value =
            motTest
    }

    /*
     * =========================================================
     * PUBLIC STATE
     * =========================================================
     */

    val uiState:
        StateFlow<List<Any>> =
        stateHolder
            .uiState
            .asStateFlow()

    val isLoading:
        StateFlow<Boolean> =
        stateHolder
            .isLoading
            .asStateFlow()


    val currentRawJson:
        StateFlow<String?> =
        stateHolder
            .currentRawJson
            .asStateFlow()

    /*
     * The official vehicle returned by the free search.
     *
     * This is the source of truth for vehicle identity when the user
     * opens an MOT test from the free search screen.
     */
    private val _currentVehicle =
        MutableStateFlow<Vehicle?>(null)

    val currentVehicle:
        StateFlow<Vehicle?> =
        _currentVehicle.asStateFlow()

    val rawAdvertInput:
        StateFlow<String> =
        stateHolder
            .rawAdvertInput
            .asStateFlow()

    val parsedAdvert =
        stateHolder
            .parsedAdvert
            .asStateFlow()

    val mappedAdvertVehicle =
        stateHolder
            .mappedAdvertVehicle
            .asStateFlow()

    val savedReports:
        StateFlow<List<HiddenHistoryReportEntity>> =
        stateHolder
            .savedReports
            .asStateFlow()

    init {
        loadSavedReports()
    }

    /*
     * =========================================================
     * BASIC STATE
     * =========================================================
     */

    fun updateRawAdvertInput(
        input: String
    ) {

        stateHolder.rawAdvertInput.value =
            input
    }

    /*
     * =========================================================
     * INVALID INPUT
     * =========================================================
     */

    private fun rejectInvalidInput(
        message: String =
            "Invalid input. Please provide a vehicle registration, " +
                "or paste the full advert text including the vehicle registration."
    ) {

        stateHolder.parsedAdvert.value =
            null

        stateHolder.mappedAdvertVehicle.value =
            null

        _officialCrossCheck.value =
            null

        /*
         * Keep the Debug Inspector synchronized.
         */

        DebugStateHolder.updateAdvert(
            null
        )

        DebugStateHolder.updateOfficialCrossCheck(
            null
        )

        stateHolder.uiState.value =
            listOf(
                message
            )
    }

    /*
     * =========================================================
     * SINGLE UNIVERSAL SEARCH
     * =========================================================
     */

    fun processUniversalInput(
        input: String
    ) {

        val trimmed =
            input.trim()

        if (
            trimmed.isEmpty()
        ) {

            rejectInvalidInput()

            return
        }

        stateHolder.rawAdvertInput.value =
            trimmed

        _officialCrossCheck.value =
            null

        /*
         * New search = new debug cross-check state.
         */

        DebugStateHolder.updateOfficialCrossCheck(
            null
        )

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            stateHolder.isLoading.value =
                true

            try {

                val isUrl =
                    trimmed.startsWith(
                        "http://",
                        ignoreCase = true
                    ) ||
                    trimmed.startsWith(
                        "https://",
                        ignoreCase = true
                    ) ||
                    trimmed.contains(
                        "www.",
                        ignoreCase = true
                    )

                val compactInput =
                    trimmed.replace(
                        Regex("\\s+"),
                        ""
                    )

                val isStandaloneRegistration =
                    !isUrl &&
                    compactInput.length in 2..8 &&
                    compactInput.all {
                        it.isLetterOrDigit()
                    } &&
                    compactInput.any {
                        it.isLetter()
                    } &&
                    compactInput.any {
                        it.isDigit()
                    }

                when {

                    /*
                     * DIRECT REGISTRATION
                     */

                    isStandaloneRegistration -> {

                        stateHolder.parsedAdvert.value =
                            null

                        stateHolder.mappedAdvertVehicle.value =
                            null

                        DebugStateHolder.updateAdvert(
                            null
                        )

                        performSearchInternal(

                            cleanPlate =
                                compactInput.uppercase(),

                            advert =
                                null
                        )
                    }

                    /*
                     * URL
                     */

                    isUrl -> {

                        rejectInvalidInput(
                            "Invalid input. Links and URLs are not accepted here. " +
                                "Please provide the vehicle registration, or paste " +
                                "the advert text including the registration."
                        )
                    }

                    /*
                     * ADVERT / OTHER TEXT
                     */

                    else -> {

                        processAdvertInputInternal(
                            trimmed
                        )
                    }
                }

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Universal input processing failed: ${e.message}",
                    e
                )

                stateHolder.uiState.value =
                    listOf(
                        "Error: ${
                            e.localizedMessage
                                ?: "Failed to process input"
                        }"
                    )

            } finally {

                stateHolder.isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * ADVERT PROCESSING
     * =========================================================
     */

    fun parseAdvertText(
        text: String
    ) {

        val trimmed =
            text.trim()

        if (
            trimmed.isEmpty()
        ) {

            rejectInvalidInput()

            return
        }

        stateHolder.rawAdvertInput.value =
            trimmed

        _officialCrossCheck.value =
            null

        DebugStateHolder.updateOfficialCrossCheck(
            null
        )

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            stateHolder.isLoading.value =
                true

            try {

                processAdvertInputInternal(
                    trimmed
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Parse advert text failed: ${e.message}",
                    e
                )

                stateHolder.uiState.value =
                    listOf(
                        "Advert Analysis Error: ${
                            e.localizedMessage
                                ?: "Failed to analyse advert"
                        }"
                    )

            } finally {

                stateHolder.isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * INTERNAL ADVERT PROCESSING
     * =========================================================
     */

    private suspend fun processAdvertInputInternal(
        text: String
    ) {

        val processResult =
            advertProcessor.processAdvert(
                text
            )

        stateHolder.parsedAdvert.value =
            processResult.parsed

        stateHolder.mappedAdvertVehicle.value =
            processResult.advertVehicle

        /*
         * =========================================================
         * DEBUG — ACTUAL ADVERT ANALYSIS
         * =========================================================
         *
         * Store the actual parsed advert result rather than relying
         * on the Debug screen trying to reconstruct it.
         */

        DebugStateHolder.updateAdvert(
            processResult.parsed
        )

        /*
         * =========================================================
         * REGISTRATION BRIDGE
         * =========================================================
         */

        if (
            processResult.extractedRegistration != null
        ) {

            performSearchInternal(

                cleanPlate =
                    processResult
                        .extractedRegistration,

                advert =
                    processResult.parsed
            )

            return
        }

        /*
         * Advert-only / description-only input is deliberately
         * rejected.
         */

        rejectInvalidInput(
            "The advert was received, but no vehicle registration was found. " +
                "Please paste the advert text including the vehicle registration, " +
                "or enter the registration separately."
        )
    }

    /*
     * =========================================================
     * VEHICLE SEARCH
     * =========================================================
     */

    fun performSearch(
        cleanPlate: String
    ) {

        val registration =
            cleanPlate
                .trim()
                .replace(
                    Regex("\\s+"),
                    ""
                )
                .uppercase()

        if (
            registration.isBlank()
        ) {

            rejectInvalidInput()

            return
        }

        _officialCrossCheck.value =
            null

        DebugStateHolder.updateOfficialCrossCheck(
            null
        )

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            stateHolder.isLoading.value =
                true

            try {

                performSearchInternal(

                    cleanPlate =
                        registration,

                    advert =
                        stateHolder
                            .parsedAdvert
                            .value
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Search failed: ${e.message}",
                    e
                )

                stateHolder.uiState.value =
                    listOf(
                        "Search Error: ${
                            e.javaClass.simpleName
                        } -> ${
                            e.localizedMessage
                                ?: "Unknown error"
                        }"
                    )

            } finally {

                stateHolder.isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * INTERNAL OFFICIAL VEHICLE SEARCH
     * =========================================================
     */

    private suspend fun performSearchInternal(
        cleanPlate: String,
        advert: ParsedVehicleAdvert? = null
    ) {

        val responseData =
            officialLookup.performLookup(
                cleanPlate
            )

        stateHolder.currentRawJson.value =
            responseData

        val rawMap =
            reportBuilder.parseJsonToMap(
                responseData
            )

        val vehicle =
            jsonParser.decodeFromString<Vehicle>(
                responseData
            )

        /*
         * Keep the complete official vehicle.
         */

        _currentVehicle.value =
            vehicle

        /*
         * =========================================================
         * OFFICIAL ADVERT CROSS-CHECK
         * =========================================================
         */

        if (
            advert != null
        ) {

            try {

                val crossCheckResult =
                    advertOfficialCrossCheckEngine
                        .compare(
                            advert = advert,
                            vehicle = vehicle
                        )

                /*
                 * Existing production state.
                 */

                _officialCrossCheck.value =
                    crossCheckResult

                /*
                 * NEW:
                 *
                 * Send the exact same cross-check result to the
                 * Debug Inspector.
                 *
                 * No second calculation.
                 * No reconstruction.
                 * No string conversion.
                 */

                DebugStateHolder
                    .updateOfficialCrossCheck(
                        crossCheckResult
                    )

                Log.d(
                    "FreeAdvertCrossCheck",
                    """
                    Advert Official Cross-Check
                    Registration: $cleanPlate

                    Warnings:
                    ${crossCheckResult.warnings.joinToString("\n")}

                    Confirmations:
                    ${crossCheckResult.confirmations.joinToString("\n")}

                    Verification:
                    ${crossCheckResult.verificationItems.joinToString("\n")}
                    """.trimIndent()
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeAdvertCrossCheck",
                    "Advert official cross-check failed: ${e.message}",
                    e
                )

                _officialCrossCheck.value =
                    null

                DebugStateHolder
                    .updateOfficialCrossCheck(
                        null
                    )
            }

        } else {

            _officialCrossCheck.value =
                null

            DebugStateHolder
                .updateOfficialCrossCheck(
                    null
                )
        }


        /*
         * =========================================================
         * OFFICIAL VEHICLE REPORT
         * =========================================================
         *
         * Hidden History no longer runs the removed legacy intelligence
         * intelligence system. The official vehicle payload is
         * displayed directly. Advert analysis and the official advert
         * cross-check remain separate production stages.
         */

        stateHolder.uiState.value =
            reportBuilder
                .mapVehicleToAdapterList(
                    vehicle
                )

        DebugStateHolder.update(
            raw =
                rawMap
        )
    }

    /*
     * =========================================================
     * LOAD SAVED REPORTS
     * =========================================================
     */

    fun loadSavedReports(
        onError: ((String) -> Unit)? = null
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val reports =
                    savedReportsHandler
                        .loadSavedReports()

                stateHolder.savedReports.value =
                    reports

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Failed to load saved reports: ${e.message}",
                    e
                )

                stateHolder.savedReports.value =
                    emptyList()

                onError?.invoke(
                    e.localizedMessage
                        ?: "Failed to load saved reports."
                )
            }
        }
    }

    /*
     * =========================================================
     * EBAY PART SEARCH
     * =========================================================
     */

    fun searchEbayPart(
        defectText: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val vehicle =
                    _currentVehicle.value

                val motTest =
                    _selectedMotTest.value

                if (
                    motTest == null
                ) {

                    throw IllegalStateException(
                        "No MOT test is selected."
                    )
                }

                val responseData =
                    partsHandler.executeEbaySearch(

                        defectText =
                            defectText,

                        vehicle =
                            vehicle,

                        motTest =
                            motTest
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    onSuccess(
                        responseData
                    )
                }

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "eBay search failed: ${e.message}",
                    e
                )

                withContext(
                    Dispatchers.Main
                ) {

                    onError(
                        e.localizedMessage
                            ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /*
     * =========================================================
     * TEST PROFILE
     * =========================================================
     */

    fun loadGrandCMaxTestProfile() {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            stateHolder.isLoading.value =
                true

            try {

                val vehicle =
                    GrandCMaxTest.createVehicle()

                val jsonString =
                    jsonParser.encodeToString(
                        vehicle
                    )

                stateHolder.currentRawJson.value =
                    jsonString

                _currentVehicle.value =
                    vehicle

                /*
                 * Test profile is not an advert.
                 */

                DebugStateHolder.updateAdvert(
                    null
                )

                DebugStateHolder.updateOfficialCrossCheck(
                    null
                )

                stateHolder.uiState.value =
                    reportBuilder
                        .mapVehicleToAdapterList(
                            vehicle
                        )

                val rawMap =
                    mapOf(
                        "registration" to vehicle.registrationNumber,
                        "make" to vehicle.make,
                        "model" to vehicle.model,
                        "price" to vehicle.price,
                        "motTestsCount" to vehicle.motTests.size
                    )

                DebugStateHolder.update(
                    raw =
                        rawMap
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Failed to load test profile: ${e.message}",
                    e
                )

                stateHolder.uiState.value =
                    listOf(
                        "Error: ${
                            e.localizedMessage
                                ?: "Failed to load test profile"
                        }"
                    )

            } finally {

                stateHolder.isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * SAVE CURRENT REPORT
     * =========================================================
     */

    fun saveCurrentReport(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val updatedReports =
                    savedReportsHandler
                        .saveCurrentReport(

                            currentRawJson =
                                stateHolder
                                    .currentRawJson
                                    .value,

                            parsedAdvert =
                                stateHolder
                                    .parsedAdvert
                                    .value,

                            officialCrossCheck =
                                _officialCrossCheck
                                    .value,

                            rawAdvertInput =
                                stateHolder
                                    .rawAdvertInput
                                    .value
                        )

                stateHolder.savedReports.value =
                    updatedReports

                withContext(
                    Dispatchers.Main
                ) {

                    onSuccess()
                }

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Failed to save vehicle report: ${e.message}",
                    e
                )

                withContext(
                    Dispatchers.Main
                ) {

                    onError(
                        e.localizedMessage
                            ?: "Failed to save vehicle report."
                    )
                }
            }
        }
    }

    /*
     * =========================================================
     * DELETE SAVED REPORT
     * =========================================================
     */

    fun deleteSavedReport(
        reportId: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val updatedReports =
                    savedReportsHandler
                        .deleteSavedReport(
                            reportId
                        )

                stateHolder.savedReports.value =
                    updatedReports

                onSuccess?.invoke()

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "FreeVehicleSearch",
                    "Failed to delete report: ${e.message}",
                    e
                )

                onError?.invoke(
                    e.localizedMessage
                        ?: "Failed to delete report."
                )
            }
        }
    }
}