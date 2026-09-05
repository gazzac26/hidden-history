package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.billing.VehicleReportTokenManager
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.engine.AdvertParserEngine
import com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine
import com.hiddenhistory.models.AdvertAnalysis
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import com.hiddenhistory.repository.AdvertAnalysisRepository
import com.hiddenhistory.repository.HiddenHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import io.github.jan.supabase.auth.auth

/*
 * =====================================================================
 * PRO SEARCH TYPE
 * =====================================================================
 */

enum class ProSearchType {

    REGISTRATION,

    ADVERT,

    REGISTRATION_AND_ADVERT
}

data class ProVehicleSearchResult(
    val summary: String,
    val displayItems: List<String>
)

class ProVehicleSearchViewModel : ViewModel() {

    /*
     * =========================================================
     * PRO TOKEN SYSTEM
     * =========================================================
     */

    private val tokenManager =
        VehicleReportTokenManager()

    private var heldProSearchTokenId: String? =
        null

    /*
     * =========================================================
     * SELECTED PRO SEARCH TYPE
     * =========================================================
     */

    private val _selectedSearchType =
        MutableStateFlow<ProSearchType?>(null)

    val selectedSearchType:
        StateFlow<ProSearchType?> =
        _selectedSearchType.asStateFlow()

    /*
     * =========================================================
     * REGISTRATION INPUT
     * =========================================================
     */

    private val _registrationInput =
        MutableStateFlow("")

    val registrationInput:
        StateFlow<String> =
        _registrationInput.asStateFlow()

    /*
     * =========================================================
     * ADVERT INPUT
     * =========================================================
     */

    private val _advertInput =
        MutableStateFlow("")

    val advertInput:
        StateFlow<String> =
        _advertInput.asStateFlow()

    /*
     * =========================================================
     * PRO STATE
     * =========================================================
     */

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading:
        StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _result =
        MutableStateFlow<ProVehicleSearchResult?>(null)

    val result:
        StateFlow<ProVehicleSearchResult?> =
        _result.asStateFlow()

    /*
     * =========================================================
     * VEHICLE SEARCH STATE
     * =========================================================
     */

    private val _uiState =
        MutableStateFlow<List<Any>>(emptyList())

    val uiState:
        StateFlow<List<Any>> =
        _uiState.asStateFlow()

    private val _currentVehicle =
        MutableStateFlow<Vehicle?>(null)

    val currentVehicle:
        StateFlow<Vehicle?> =
        _currentVehicle.asStateFlow()

    private val _selectedMotTest =
        MutableStateFlow<MotTest?>(null)

    val selectedMotTest:
        StateFlow<MotTest?> =
        _selectedMotTest.asStateFlow()

    /*
     * =========================================================
     * LEGACY ANALYSIS RESULT STATE
     * =========================================================
     */

    private val _currentAnalysisResult =
        MutableStateFlow<Any?>(null)

    val currentAnalysisResult:
        StateFlow<Any?> =
        _currentAnalysisResult.asStateFlow()

    /*
     * =========================================================
     * RAW OFFICIAL VEHICLE JSON
     * =========================================================
     */

    private val _currentRawJson =
        MutableStateFlow<String?>(null)

    val currentRawJson:
        StateFlow<String?> =
        _currentRawJson.asStateFlow()

    /*
     * =========================================================
     * PRO ADVERT ANALYSIS
     * =========================================================
     */

    private val _advertAnalysis =
        MutableStateFlow<AdvertAnalysis?>(null)

    val advertAnalysis:
        StateFlow<AdvertAnalysis?> =
        _advertAnalysis.asStateFlow()

    /*
     * =========================================================
     * RAW ADVERT INPUT
     * =========================================================
     */

    private val _rawAdvertInput =
        MutableStateFlow("")

    val rawAdvertInput:
        StateFlow<String> =
        _rawAdvertInput.asStateFlow()

    /*
     * =========================================================
     * SAVE REPORT STATE
     * =========================================================
     */

    private val _isSaving =
        MutableStateFlow(false)

    val isSaving:
        StateFlow<Boolean> =
        _isSaving.asStateFlow()

    private val _saveMessage =
        MutableStateFlow<String?>(null)

    val saveMessage:
        StateFlow<String?> =
        _saveMessage.asStateFlow()

    /*
     * =========================================================
     * CAN SAVE CURRENT REPORT
     * =========================================================
     */

    private val _canSaveCurrentReport =
        MutableStateFlow(false)

    val canSaveCurrentReport:
        StateFlow<Boolean> =
        _canSaveCurrentReport.asStateFlow()

    /*
     * =========================================================
     * NETWORK / JSON
     * =========================================================
     */

    private val client =
        OkHttpClient()

    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    /*
     * =========================================================
     * OFFICIAL VEHICLE LOOKUP
     * =========================================================
     */

    private val officialLookup =
        VehicleSearchOfficialLookup(
            client
        )

    /*
     * =========================================================
     * ADVERT ANALYSIS REPOSITORY
     * =========================================================
     */

    private val advertAnalysisRepository =
        AdvertAnalysisRepository(
            SupabaseManager.client
        )

    /*
     * =========================================================
     * EXISTING ADVERT ENGINE
     * =========================================================
     */

    private val advertParser =
        AdvertParserEngine()

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK
     * =========================================================
     */

    private val advertOfficialCrossCheckEngine =
        AdvertOfficialCrossCheckEngine()

    /*
     * =========================================================
     * HIDDEN HISTORY SAVE PATH
     * =========================================================
     */

    private val hiddenHistoryRepository =
        HiddenHistoryRepository(
            SupabaseManager.client
        )

    private val savedReports =
        VehicleSearchSavedReports(
            hiddenHistoryRepository =
                hiddenHistoryRepository,

            jsonParser =
                jsonParser
        )

    /*
     * =========================================================
     * UK REGISTRATION VALIDATION
     * =========================================================
     */

    private val ukModernRegPattern =
        Regex(
            pattern =
                """^[A-Z]{2}[0-9]{2}[A-Z]{3}$""",

            options =
                setOf(
                    RegexOption.IGNORE_CASE
                )
        )

    /*
     * =========================================================
     * SELECT SEARCH TYPE
     * =========================================================
     */

    fun selectSearchType(
        type: ProSearchType
    ) {

        if (
            _isLoading.value
        ) {
            return
        }

        _selectedSearchType.value =
            type

        clearResultsForNewSearch()
    }

    /*
     * =========================================================
     * UPDATE REGISTRATION
     * =========================================================
     */

    fun updateRegistrationInput(
        input: String
    ) {

        if (
            _isLoading.value
        ) {
            return
        }

        _registrationInput.value =
            input.uppercase()
    }

    /*
     * =========================================================
     * UPDATE ADVERT
     * =========================================================
     */

    fun updateAdvertInput(
        input: String
    ) {

        if (
            _isLoading.value
        ) {
            return
        }

        _advertInput.value =
            input

        _rawAdvertInput.value =
            input
    }

    /*
     * =========================================================
     * COMPATIBILITY METHOD
     * =========================================================
     */

    fun updateRawAdvertInput(
        input: String
    ) {

        updateAdvertInput(
            input
        )
    }

    /*
     * =========================================================
     * CAN START SELECTED SEARCH
     * =========================================================
     */

    fun canStartSelectedSearch(): Boolean {

        return when (
            _selectedSearchType.value
        ) {

            ProSearchType.REGISTRATION -> {

                isValidRegistration(
                    _registrationInput.value
                )
            }

            ProSearchType.ADVERT -> {

                _advertInput.value
                    .trim()
                    .isNotBlank()
            }

            ProSearchType.REGISTRATION_AND_ADVERT -> {

                isValidRegistration(
                    _registrationInput.value
                ) &&
                    _advertInput.value
                        .trim()
                        .isNotBlank()
            }

            null -> {

                false
            }
        }
    }

    /*
     * =========================================================
     * REGISTRATION VALIDATION
     * =========================================================
     */

    private fun isValidRegistration(
        registration: String
    ): Boolean {

        val compact =
            registration
                .replace(
                    Regex("\\s+"),
                    ""
                )
                .uppercase()

        return ukModernRegPattern.matches(
            compact
        )
    }

    /*
     * =========================================================
     * NORMALISE REGISTRATION
     * =========================================================
     */

    private fun normaliseRegistration(
        registration: String
    ): String {

        return registration
            .trim()
            .replace(
                Regex("\\s+"),
                ""
            )
            .uppercase()
    }

    /*
     * =========================================================
     * CLEAR RESULTS
     * =========================================================
     */

    private fun clearResultsForNewSearch() {

        _result.value =
            null

        _uiState.value =
            emptyList()

        _currentVehicle.value =
            null

        _currentAnalysisResult.value =
            null

        _currentRawJson.value =
            null

        _advertAnalysis.value =
            null

        _selectedMotTest.value =
            null

        _saveMessage.value =
            null

        _canSaveCurrentReport.value =
            false
    }

    /*
     * =========================================================
     * SELECT MOT TEST
     * =========================================================
     */

    fun selectMotTest(
        motTest: MotTest?
    ) {

        _selectedMotTest.value =
            motTest
    }

    /*
     * =========================================================
     * CLEAR SAVE MESSAGE
     * =========================================================
     */

    fun clearSaveMessage() {

        _saveMessage.value =
            null
    }

    /*
     * =========================================================
     * DETERMINE WHETHER CURRENT REPORT IS SAVEABLE
     * =========================================================
     */

    private fun hasSaveableReport(): Boolean {

        val hasOfficialVehicleReport =
            _currentVehicle.value != null &&
                !_currentRawJson.value.isNullOrBlank()

        val hasProAdvertReport =
            _advertAnalysis.value != null

        return hasOfficialVehicleReport ||
            hasProAdvertReport
    }

    /*
     * =========================================================
     * SAVE CURRENT REPORT
     * =========================================================
     */

    fun saveCurrentReport() {

        val vehicle =
            _currentVehicle.value

        val rawJson =
            _currentRawJson.value

        val currentAdvertAnalysis =
            _advertAnalysis.value

        val hasOfficialVehicleReport =
            vehicle != null &&
                !rawJson.isNullOrBlank()

        val hasProAdvertReport =
            currentAdvertAnalysis != null

        if (
            !hasOfficialVehicleReport &&
            !hasProAdvertReport
        ) {

            _canSaveCurrentReport.value =
                false

            _saveMessage.value =
                "No completed report is available to save."

            return
        }

        if (
            _isSaving.value
        ) {
            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            _isSaving.value =
                true

            _saveMessage.value =
                null

            try {

                Log.d(
                    "ProVehicleSearch",
                    "Saving completed report. " +
                        "officialVehicle=$hasOfficialVehicleReport " +
                        "advertOnly=$hasProAdvertReport"
                )

                val rawAdvert =
                    _advertInput.value
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }

                val parsedAdvert =
                    rawAdvert
                        ?.let {

                            try {

                                advertParser.parse(
                                    it
                                )

                            } catch (
                                e: Throwable
                            ) {

                                Log.w(
                                    "ProVehicleSearch",
                                    "Advert parsing during save failed: ${e.message}"
                                )

                                null
                            }
                        }
                        ?.takeIf {
                            it.rawText.isNotBlank()
                        }

                val officialCrossCheck =
                    if (
                        vehicle != null &&
                        parsedAdvert != null
                    ) {

                        try {

                            advertOfficialCrossCheckEngine.compare(
                                advert =
                                    parsedAdvert,

                                vehicle =
                                    vehicle
                            )

                        } catch (
                            e: Throwable
                        ) {

                            Log.w(
                                "ProVehicleSearch",
                                "Official advert cross-check failed during save: ${e.message}"
                            )

                            null
                        }

                    } else {

                        null
                    }

                savedReports.saveCurrentReport(

                    currentRawJson =
                        if (
                            hasOfficialVehicleReport
                        ) {
                            rawJson
                        } else {
                            null
                        },

                    parsedAdvert =
                        parsedAdvert,

                    advertAnalysis =
                        currentAdvertAnalysis,

                    officialCrossCheck =
                        officialCrossCheck,

                    rawAdvertInput =
                        rawAdvert
                )

                Log.d(
                    "ProVehicleSearch",
                    "Report saved successfully."
                )

                _saveMessage.value =
                    if (
                        hasOfficialVehicleReport
                    ) {

                        "Report saved successfully."

                    } else {

                        "Pro advert report saved successfully."
                    }

                _canSaveCurrentReport.value =
                    hasSaveableReport()

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Failed to save report: ${e.message}",
                    e
                )

                _saveMessage.value =
                    e.localizedMessage
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Failed to save report."

                _canSaveCurrentReport.value =
                    hasSaveableReport()

            } finally {

                _isSaving.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * RESERVE PRO SEARCH TOKEN
     * =========================================================
     */

    private suspend fun reserveProSearchToken(): String {

        heldProSearchTokenId?.let {

            Log.d(
                "ProVehicleSearch",
                "Existing Pro Vehicle Search token already held: $it"
            )

            return it
        }

        Log.d(
            "ProVehicleSearch",
            "Reserving one Pro Vehicle Search token..."
        )

        val tokenResult =
            tokenManager.reserveToken()

        val tokenId =
            tokenResult.getOrThrow()

        if (
            tokenId.isBlank()
        ) {

            throw IllegalStateException(
                "The Pro Vehicle Search token reservation returned an empty token ID."
            )
        }

        heldProSearchTokenId =
            tokenId

        Log.d(
            "ProVehicleSearch",
            "Pro Vehicle Search token reserved: $tokenId"
        )

        return tokenId
    }

    /*
     * =========================================================
     * CONSUME PRO SEARCH TOKEN
     * =========================================================
     */

    private suspend fun consumeProSearchToken() {

        val tokenId =
            heldProSearchTokenId
                ?: throw IllegalStateException(
                    "No Pro Vehicle Search token is currently held."
                )

        Log.d(
            "ProVehicleSearch",
            "Consuming Pro Vehicle Search token: $tokenId"
        )

        val result =
            tokenManager.consumeToken(
                tokenId
            )

        val consumed =
            result.getOrThrow()

        if (
            !consumed
        ) {

            throw IllegalStateException(
                "The Pro Vehicle Search token could not be completed."
            )
        }

        heldProSearchTokenId =
            null

        Log.d(
            "ProVehicleSearch",
            "Pro Vehicle Search token consumed successfully: $tokenId"
        )
    }

    /*
     * =========================================================
     * REFUND PRO SEARCH TOKEN
     * =========================================================
     */

    private suspend fun refundProSearchToken() {

        val tokenId =
            heldProSearchTokenId
                ?: return

        Log.d(
            "ProVehicleSearch",
            "Refunding Pro Vehicle Search token: $tokenId"
        )

        try {

            val result =
                tokenManager.refundToken(
                    tokenId
                )

            val refunded =
                result.getOrElse {
                    false
                }

            if (
                refunded
            ) {

                Log.d(
                    "ProVehicleSearch",
                    "Pro Vehicle Search token refunded successfully: $tokenId"
                )

                heldProSearchTokenId =
                    null

            } else {

                Log.e(
                    "ProVehicleSearch",
                    "Pro Vehicle Search token refund was not confirmed: $tokenId"
                )
            }

        } catch (
            e: Throwable
        ) {

            Log.e(
                "ProVehicleSearch",
                "Failed to refund Pro Vehicle Search token: ${e.message}",
                e
            )
        }
    }

    /*
     * =========================================================
     * START REGISTRATION SEARCH
     * =========================================================
     */

    fun processRegistrationSearch() {

        if (
            _isLoading.value
        ) {
            return
        }

        if (
            _selectedSearchType.value !=
                ProSearchType.REGISTRATION
        ) {
            return
        }

        val cleanPlate =
            normaliseRegistration(
                _registrationInput.value
            )

        if (
            !isValidRegistration(
                cleanPlate
            )
        ) {

            _uiState.value =
                listOf(
                    "Please enter a valid UK registration."
                )

            return
        }

        runSelectedProSearch {

            processRegistrationInputInternal(
                cleanPlate
            )
        }
    }

    /*
     * =========================================================
     * START ADVERT SEARCH
     * =========================================================
     */

    fun processAdvertSearch() {

        if (
            _isLoading.value
        ) {
            return
        }

        if (
            _selectedSearchType.value !=
                ProSearchType.ADVERT
        ) {
            return
        }

        val advert =
            _advertInput.value
                .trim()

        if (
            advert.isBlank()
        ) {

            _uiState.value =
                listOf(
                    "Please paste a vehicle advert."
                )

            return
        }

        runSelectedProSearch {

            processAdvertOnlyInputInternal(
                advert
            )
        }
    }

    /*
     * =========================================================
     * START REGISTRATION + ADVERT SEARCH
     * =========================================================
     */

    fun processRegistrationAndAdvertSearch() {

        if (
            _isLoading.value
        ) {
            return
        }

        if (
            _selectedSearchType.value !=
                ProSearchType.REGISTRATION_AND_ADVERT
        ) {
            return
        }

        val cleanPlate =
            normaliseRegistration(
                _registrationInput.value
            )

        val advert =
            _advertInput.value
                .trim()

        if (
            !isValidRegistration(
                cleanPlate
            )
        ) {

            _uiState.value =
                listOf(
                    "Please enter a valid UK registration."
                )

            return
        }

        if (
            advert.isBlank()
        ) {

            _uiState.value =
                listOf(
                    "Please paste a vehicle advert."
                )

            return
        }

        runSelectedProSearch {

            processRegistrationAndAdvertInputInternal(
                cleanPlate,
                advert
            )
        }
    }

    /*
     * =========================================================
     * COMMON SELECTED PRO SEARCH EXECUTOR
     * =========================================================
     */

    private fun runSelectedProSearch(
        pipeline: suspend () -> Unit
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            val currentSession =
                SupabaseManager.client.auth.currentSessionOrNull()

            if (currentSession == null) {

                Log.d(
                    "ProVehicleSearch",
                    "Pro Search blocked: no authenticated user session."
                )

                _uiState.value =
                    listOf(
                        "Please log in to use Pro Search."
                    )

                _canSaveCurrentReport.value =
                    false

                return@launch
            }

            _isLoading.value =
                true

            try {

                clearResultsForNewSearch()

                reserveProSearchToken()

                pipeline()

                consumeProSearchToken()

                Log.d(
                    "ProVehicleSearch",
                    "Pro Search completed successfully and token consumption was confirmed."
                )

                _canSaveCurrentReport.value =
                    hasSaveableReport()

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Selected Pro Search failed: ${e.message}",
                    e
                )

                refundProSearchToken()

                val errorMessage =
                    if (
                        e.message?.contains(
                            "No vehicle report token is available"
                        ) == true
                    ) {

                        "No Pro Vehicle Search tokens are available."

                    } else if (
                        e.message?.contains("403") == true ||
                        e.message?.contains("PRO_REQUIRED") == true ||
                        e.message?.contains("Pro access required") == true
                    ) {

                        "Access denied."

                    } else {

                        "Pro Search Error: ${
                            e.localizedMessage
                                ?: "Failed to process input"
                        }"
                    }

                _uiState.value =
                    listOf(errorMessage)

                _canSaveCurrentReport.value =
                    false

            } finally {

                _isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * REGISTRATION-ONLY PIPELINE
     * =========================================================
     */

    private suspend fun processRegistrationInputInternal(
        cleanPlate: String
    ) {

        Log.d(
            "ProVehicleSearch",
            "Starting Pro registration search: $cleanPlate"
        )

        retrieveOfficialVehicleData(
            cleanPlate
        )

        _advertAnalysis.value =
            null

        _result.value =
            ProVehicleSearchResult(

                summary =
                    "Pro vehicle search completed.",

                displayItems =
                    listOf(

                        "Registration: ${
                            _currentVehicle.value
                                ?.registrationNumber
                                ?: cleanPlate
                        }",

                        "DVLA + DVSA/MOT",

                        "Vehicle analysis"
                    )
            )

        Log.d(
            "ProVehicleSearch",
            "Pro registration search completed: $cleanPlate"
        )
    }

    /*
     * =========================================================
     * ADVERT-ONLY PIPELINE
     * =========================================================
     */

    private suspend fun processAdvertOnlyInputInternal(
        text: String
    ) {

        Log.d(
            "ProVehicleSearch",
            "Starting Pro advert-only pipeline."
        )

        val parsedAdvert =
            try {

                advertParser.parse(
                    text
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Advert parsing failed: ${e.message}",
                    e
                )

                null
            }

        val analysis =
            advertAnalysisRepository.analyzeAdvert(

                advertText =
                    text,

                registration =
                    null,

                officialVehicleData =
                    null,

                deterministicAdvertAnalysis =
                    parsedAdvert
            )

        _advertAnalysis.value =
            analysis

        _currentVehicle.value =
            null

        _currentRawJson.value =
            null

        _currentAnalysisResult.value =
            null

        _uiState.value =
            emptyList()

        _result.value =
            ProVehicleSearchResult(

                summary =
                    "Pro advert analysis completed.",

                displayItems =
                    buildList {

                        add(
                            "Deterministic advert analysis"
                        )

                        add(
                            "Gemini Pro analysis"
                        )

                        add(
                            "Pro advert report available to save"
                        )
                    }
            )

        _canSaveCurrentReport.value =
            hasSaveableReport()

        Log.d(
            "ProVehicleSearch",
            "Pro advert-only pipeline completed. " +
                "saveable=${_canSaveCurrentReport.value}"
        )
    }

    /*
     * =========================================================
     * REGISTRATION + ADVERT PIPELINE
     * =========================================================
     */

    private suspend fun processRegistrationAndAdvertInputInternal(
        cleanPlate: String,
        text: String
    ) {

        Log.d(
            "ProVehicleSearch",
            "Starting Pro registration + advert pipeline."
        )

        val parsedAdvert =
            try {

                advertParser.parse(
                    text
                )

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Advert parsing failed: ${e.message}",
                    e
                )

                null
            }

        val officialVehicleData =
            retrieveOfficialVehicleData(
                cleanPlate
            )

        val analysis =
            advertAnalysisRepository.analyzeAdvert(

                advertText =
                    text,

                registration =
                    cleanPlate,

                officialVehicleData =
                    officialVehicleData,

                deterministicAdvertAnalysis =
                    parsedAdvert
            )

        _advertAnalysis.value =
            analysis

        _result.value =
            ProVehicleSearchResult(

                summary =
                    "Pro registration + advert search completed.",

                displayItems =
                    buildList {

                        add(
                            "Registration: $cleanPlate"
                        )

                        add(
                            "DVLA + DVSA/MOT"
                        )

                        add(
                            "Vehicle analysis"
                        )

                        add(
                            "Deterministic advert analysis"
                        )

                        add(
                            "Gemini Pro analysis"
                        )
                    }
            )

        _canSaveCurrentReport.value =
            hasSaveableReport()

        Log.d(
            "ProVehicleSearch",
            "Pro registration + advert pipeline completed."
        )
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE LOOKUP
     * =========================================================
     */

    private suspend fun retrieveOfficialVehicleData(
        cleanPlate: String
    ): String {

        Log.d(
            "ProVehicleSearch",
            "Starting official vehicle lookup: $cleanPlate"
        )

        val responseData =
            officialLookup.performLookup(
                cleanPlate
            )

        _currentRawJson.value =
            responseData

        try {

            val vehicle =
                jsonParser.decodeFromString<Vehicle>(
                    responseData
                )

            _currentVehicle.value =
                vehicle

            _uiState.value =
                mapVehicleToUiState(
                    vehicle
                )

        } catch (
            e: Throwable
        ) {

            Log.e(
                "ProVehicleSearch",
                "Failed to parse official vehicle data: ${e.message}",
                e
            )

            _currentVehicle.value =
                null

            _uiState.value =
                listOf(
                    "Official vehicle data was retrieved but could not be displayed."
                )

            throw IllegalStateException(
                "Official vehicle data was retrieved but could not be processed.",
                e
            )
        }

        Log.d(
            "ProVehicleSearch",
            "Official vehicle lookup completed: $cleanPlate"
        )

        return responseData
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE → UI
     * =========================================================
     */

    private fun mapVehicleToUiState(
        vehicle: Vehicle
    ): List<Any> {

        val list =
            mutableListOf<Any>()

        list.add(
            "Vehicle Identity"
        )

        vehicle.registrationNumber?.let {

            list.add(
                "Registration" to it
            )
        }

        vehicle.registration?.let {

            if (
                it != vehicle.registrationNumber
            ) {

                list.add(
                    "Alt Registration" to it
                )
            }
        }

        vehicle.make?.let {

            list.add(
                "Make" to it
            )
        }

        vehicle.model?.let {

            list.add(
                "Model" to it
            )
        }

        vehicle.year?.let {

            list.add(
                "Year of Manufacture" to
                    it.toString()
            )
        }

        vehicle.vin?.let {

            list.add(
                "VIN" to it
            )
        }

        vehicle.engineSize?.let {

            list.add(
                "Engine Size" to it
            )
        }

        vehicle.colour?.let {

            list.add(
                "Colour" to it
            )
        }

        vehicle.primaryColour?.let {

            list.add(
                "Primary Colour" to it
            )
        }

        vehicle.wheelplan?.let {

            list.add(
                "Wheelplan" to it
            )
        }

        list.add(
            "Technical Specifications"
        )

        vehicle.engineCapacity?.let {

            list.add(
                "Engine Capacity" to
                    "$it cc"
            )
        }

        vehicle.fuelType?.let {

            list.add(
                "Fuel Type" to it
            )
        }

        vehicle.co2Emissions?.let {

            list.add(
                "CO₂ Emissions" to
                    "$it g/km"
            )
        }

        vehicle.typeApproval?.let {

            list.add(
                "Type Approval" to it
            )
        }

        vehicle.seats?.let {

            list.add(
                "Seats" to
                    it.toString()
            )
        }

        vehicle.maxTowWeight?.let {

            list.add(
                "Maximum Tow Weight" to
                    it.toString()
            )
        }

        list.add(
            "Registration Information"
        )

        vehicle.registrationDate?.let {

            list.add(
                "Registration Date" to it
            )
        }

        vehicle.monthOfFirstRegistration?.let {

            list.add(
                "Month of First Registration" to it
            )
        }

        vehicle.manufactureDate?.let {

            list.add(
                "Manufacture Date" to it
            )
        }

        vehicle.firstUsedDate?.let {

            list.add(
                "First Used Date" to it
            )
        }

        vehicle.dateOfLastV5CIssued?.let {

            list.add(
                "Last V5C Issued" to it
            )
        }

        list.add(
            "Tax & MOT"
        )

        vehicle.taxStatus?.let {

            list.add(
                "Tax Status" to it
            )
        }

        vehicle.taxDueDate?.let {

            list.add(
                "Tax Due Date" to it
            )
        }

        vehicle.motStatus?.let {

            list.add(
                "MOT Status" to it
            )
        }

        vehicle.motExpiryDate?.let {

            list.add(
                "MOT Expiry Date" to it
            )
        }

        list.add(
            "Vehicle History"
        )

        vehicle.previousKeepers?.let {

            list.add(
                "Previous Keepers" to
                    it.toString()
            )
        }

        vehicle.previousOwners?.let {

            list.add(
                "Previous Owners" to
                    it.toString()
            )
        }

        vehicle.hasOutstandingRecall?.let {

            list.add(
                "Outstanding Recall" to it
            )
        }

        vehicle.salvageCategory?.let {

            list.add(
                "Salvage Category" to it
            )
        }

        vehicle.vehicleTier?.let {

            list.add(
                "Vehicle Tier" to it
            )
        }

        vehicle.markedForExport?.let {

            list.add(
                "Marked for Export" to
                    it.toString()
            )
        }

        if (
            vehicle.motTests.isNotEmpty()
        ) {

            list.add(
                "MOT History"
            )

            vehicle.motTests.forEach {
                list.add(it)
            }
        }

        if (
            vehicle.activeSymptoms.isNotEmpty()
        ) {

            list.add(
                "Active Symptoms"
            )

            vehicle.activeSymptoms.forEach {
                list.add(it)
            }
        }

        return list
    }

    /*
     * =========================================================
     * RESET
     * =========================================================
     */

    fun resetState() {

        if (
            !_isLoading.value
        ) {

            _selectedSearchType.value =
                null

            _registrationInput.value =
                ""

            _advertInput.value =
                ""

            _rawAdvertInput.value =
                ""

            _result.value =
                null

            _uiState.value =
                emptyList()

            _currentVehicle.value =
                null

            _currentAnalysisResult.value =
                null

            _currentRawJson.value =
                null

            _advertAnalysis.value =
                null

            _selectedMotTest.value =
                null

            _saveMessage.value =
                null

            _isSaving.value =
                false

            _canSaveCurrentReport.value =
                false
        }
    }

    /*
     * =========================================================
     * LEGACY SEARCH COMPATIBILITY
     * =========================================================
     */

    fun search(
        registration: String
    ) {

        if (
            _isLoading.value
        ) {
            return
        }

        _selectedSearchType.value =
            ProSearchType.REGISTRATION

        _registrationInput.value =
            registration

        processRegistrationSearch()
    }

    /*
     * =========================================================
     * VIEWMODEL CLEANUP
     * =========================================================
     */

    override fun onCleared() {

        super.onCleared()
    }
}