package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.engine.AdvertParserEngine
import com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine
import com.hiddenhistory.models.AdvertAnalysis
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.SymptomReport
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

data class ProVehicleSearchResult(
    val summary: String,
    val displayItems: List<String>
)

class ProVehicleSearchViewModel : ViewModel() {

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
     *
     * Retained for compatibility with existing callers.
     *
     * IMPORTANT:
     *
     * Pro Search no longer generates the removed legacy vehicle intelligence result through the
     * old AutoApp intelligence pipeline.
     *
     * Pro analysis is now provided by AdvertAnalysis.
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
     * ADVERT ANALYSIS
     * =========================================================
     *
     * This is the actual Pro analysis result.
     *
     * Pro Search uses the SAME AdvertAnalysisRepository used by
     * AdvertAnalysisViewModel.
     *
     * Therefore both pathways ultimately use:
     *
     * AdvertAnalysisRepository
     *          ↓
     * Supabase
     *          ↓
     * analyse-advert Edge Function
     *
     * There is NO AutoApp intelligence coordinator involved.
     */

    private val _advertAnalysis =
        MutableStateFlow<AdvertAnalysis?>(null)

    val advertAnalysis:
        StateFlow<AdvertAnalysis?> =
        _advertAnalysis.asStateFlow()

    /*
     * =========================================================
     * RAW INPUT
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
     *
     * This is ONLY responsible for retrieving the official
     * DVLA + DVSA/MOT vehicle payload.
     *
     * It does NOT perform analysis.
     */

    private val officialLookup =
        VehicleSearchOfficialLookup(
            client
        )

    /*
     * =========================================================
     * ADVERT ANALYSIS REPOSITORY
     * =========================================================
     *
     * IMPORTANT:
     *
     * This is the same repository used by AdvertAnalysisViewModel.
     *
     * We deliberately do NOT create or instantiate an
     * AdvertAnalysisViewModel here.
     *
     * ViewModels should not depend directly on other ViewModels.
     *
     * Both ViewModels use the same repository / Edge Function.
     */

    private val advertAnalysisRepository =
        AdvertAnalysisRepository(
            SupabaseManager.client
        )

    private val advertParser =
        AdvertParserEngine()

    private val advertOfficialCrossCheckEngine =
        AdvertOfficialCrossCheckEngine()

    /*
     * =========================================================
     * HIDDEN HISTORY SAVE PATH
     * =========================================================
     *
     * Existing permanent Hidden History storage pathway:
     *
     * ProVehicleSearchViewModel
     *          ↓
     * VehicleSearchSavedReports
     *          ↓
     * HiddenHistoryRepository
     *          ↓
     * Supabase
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
     * REGISTRATION EXTRACTION
     * =========================================================
     */

    private val ukRegPattern =
        Regex(
            pattern =
                "\\b(?:[A-Z]{2}[0-9]{2}\\s?[A-Z]{3}|[A-Z]{1}[0-9]{1,3}[A-Z]{3}|[A-Z]{3}[0-9]{1,3}[A-Z]{1}|[A-Z]{1,3}[0-9]{1,3}|[0-9]{1,4}[A-Z]{1,2}|[A-Z]{1,2}[0-9]{1,4}|[A-Z]{3}[0-9]{1,3}[A-Z])\\b",

            option =
                RegexOption.IGNORE_CASE
        )

        private fun extractRegistration(
        text: String
    ): String? {

        val match =
            ukRegPattern.find(
                text
            )

        val candidate = match
            ?.value
            ?.uppercase()
            ?.replace(
                Regex("\\s+"),
                ""
            )

        // Prevent engine capacities like "2.0L" from being extracted as registrations
        if (
            candidate.isNullOrBlank() ||
            candidate.length < 5 ||
            candidate.contains("L") && candidate.length <= 3
        ) {
            return null
        }

        return candidate
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
     * UPDATE INPUT
     * =========================================================
     */

    fun updateRawAdvertInput(
        input: String
    ) {

        _rawAdvertInput.value =
            input
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
     * SAVE CURRENT REPORT
     * =========================================================
     *
     * Uses the existing Hidden History permanent storage pathway.
     *
     * This does NOT create another Supabase save implementation.
     */

    fun saveCurrentReport() {

        val vehicle =
            _currentVehicle.value

        val rawJson =
            _currentRawJson.value

        if (
            vehicle == null ||
            rawJson.isNullOrBlank()
        ) {

            _saveMessage.value =
                "No official vehicle report is available to save."

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
                    "Saving current Pro vehicle report..."
                )

                val rawAdvertInput =
                    _rawAdvertInput.value
                        .takeIf {
                            it.isNotBlank()
                        }

                val parsedAdvert =
                    rawAdvertInput
                        ?.let {
                            advertParser.parse(it)
                        }
                        ?.takeIf {
                            it.rawText.isNotBlank()
                        }

                val officialCrossCheck =
                    parsedAdvert
                        ?.let { advert ->
                            advertOfficialCrossCheckEngine.compare(
                                advert = advert,
                                vehicle = vehicle
                            )
                        }

                savedReports.saveCurrentReport(

                    currentRawJson =
                        rawJson,

                    parsedAdvert =
                        parsedAdvert,

                    advertAnalysis =
                        _advertAnalysis.value,

                    officialCrossCheck =
                        officialCrossCheck,

                    rawAdvertInput =
                        rawAdvertInput
                )

                Log.d(
                    "ProVehicleSearch",
                    "Pro vehicle report saved successfully."
                )

                _saveMessage.value =
                    "Report saved successfully."

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Failed to save Pro vehicle report: ${e.message}",
                    e
                )

                _saveMessage.value =
                    e.localizedMessage
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Failed to save report."

            } finally {

                _isSaving.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * UNIVERSAL INPUT
     * =========================================================
     *
     * Supported:
     *
     * 1. Registration only
     * 2. Full advert containing registration
     * 3. Full advert without registration
     *
     * IMPORTANT:
     *
     * All analysis ultimately goes through AdvertAnalysisRepository.
     *
     * The old AutoApp intelligence pipeline is NOT called.
     */

    fun processUniversalInput(
        input: String
    ) {

        val trimmed =
            input.trim()

        if (
            trimmed.isEmpty()
        ) {
            return
        }

        _rawAdvertInput.value =
            trimmed

        if (
            _isLoading.value
        ) {
            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            _isLoading.value =
                true

            try {

                                val compactInput =
                    trimmed.replace(
                        Regex("\\s+"),
                        ""
                    ).uppercase()

                // Strict UK registration pattern check for standalone input
                val strictUkPlateRegex = Regex("^[A-Z]{1,3}[0-9]{1,4}[A-Z]{0,3}$|^[0-9]{1,4}[A-Z]{1,3}$|^[A-Z]{1,3}[0-9]{1,4}$")
                val isStandaloneRegistration =
                    compactInput.length in 2..8 &&
                    strictUkPlateRegex.matches(compactInput)


                /*
                 * -------------------------------------------------
                 * RESET PREVIOUS SEARCH STATE
                 * -------------------------------------------------
                 */

                _advertAnalysis.value =
                    null

                _currentAnalysisResult.value =
                    null

                _result.value =
                    null

                /*
                 * -------------------------------------------------
                 * REGISTRATION ONLY
                 * -------------------------------------------------
                 *
                 * First obtain the official vehicle data.
                 *
                 * Then send that official data through the SAME
                 * analyse-advert pathway used by AdvertAnalysisVM.
                 */

                if (
                    isStandaloneRegistration
                ) {

                    processRegistrationInputInternal(
                        compactInput.uppercase()
                    )

                } else {

                    /*
                     * -------------------------------------------------
                     * FULL ADVERT
                     * -------------------------------------------------
                     */

                    processAdvertInputInternal(
                        trimmed
                    )
                }

            } catch (
                e: Throwable
            ) {

                Log.e(
                    "ProVehicleSearch",
                    "Universal input processing failed: ${e.message}",
                    e
                )

                _uiState.value =
                    listOf(
                        "Pro Search Error: ${
                            e.localizedMessage
                                ?: "Failed to process input"
                        }"
                    )

            } finally {

                _isLoading.value =
                    false
            }
        }
    }

    /*
     * =========================================================
     * COMPATIBILITY SEARCH METHOD
     * =========================================================
     */

    fun search(
        registration: String
    ) {

        processUniversalInput(
            registration
        )
    }

    /*
     * =========================================================
     * REGISTRATION-ONLY PRO SEARCH
     * =========================================================
     *
     * FLOW:
     *
     * REGISTRATION
     *      ↓
     * VehicleSearchOfficialLookup
     *      ↓
     * DVLA + DVSA/MOT
     *
     * There is deliberately NO:
     *
     * AdvertAnalysisRepository
     * analyse-advert
     * VehicleKnowledgeCoordinator
     * KnowledgeRequestBuilder
     * VehicleSearchReportBuilder
     * legacy intelligence coordinator
     * legacy intelligence result generation
     */

    private suspend fun processRegistrationInputInternal(
        cleanPlate: String
    ) {

        Log.d(
            "ProVehicleSearch",
            "Starting Pro registration search: $cleanPlate"
        )

        /*
         * ---------------------------------------------------------
         * OFFICIAL LOOKUP ONLY (Smooth Handler)
         * ---------------------------------------------------------
         */

        retrieveOfficialVehicleData(
            cleanPlate
        )

        /*
         * ---------------------------------------------------------
         * CLEAR ADVERT ANALYSIS FOR REGISTRATION-ONLY SEARCHES
         * ---------------------------------------------------------
         */

        _advertAnalysis.value = null

        /*
         * ---------------------------------------------------------
         * UPDATE RESULT
         * ---------------------------------------------------------
         */

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

                        "DVLA + DVSA/MOT"
                    )
            )

        Log.d(
            "ProVehicleSearch",
            "Pro registration search completed: $cleanPlate"
        )
    }

    /*
     * =========================================================
     * ADVERT PROCESSING
     * =========================================================
     *
     * FLOW:
     *
     * ADVERT
     *      ↓
     * EXTRACT REGISTRATION
     *      ↓
     * OFFICIAL DVLA/DVSA LOOKUP
     *      ↓
     * AdvertAnalysisRepository
     *      ↓
     * analyse-advert
     *      ↓
     * AdvertAnalysis
     *
     * If no registration exists, the advert is still sent through
     * Advert Analysis without official vehicle data.
     */

    private suspend fun processAdvertInputInternal(
        text: String
    ) {

        val detectedRegistration =
            extractRegistration(
                text
            )

        Log.d(
            "ProVehicleSearch",
            "Advert registration detected: ${
                detectedRegistration
                    ?: "NONE"
            }"
        )

        /*
         * ---------------------------------------------------------
         * OFFICIAL DATA
         * ---------------------------------------------------------
         */

        val officialVehicleData =
            if (
                detectedRegistration != null
            ) {

                retrieveOfficialVehicleData(
                    detectedRegistration
                )

            } else {

                _currentVehicle.value =
                    null

                _currentRawJson.value =
                    null

                null
            }

        /*
         * ---------------------------------------------------------
         * ADVERT ANALYSIS
         * ---------------------------------------------------------
         *
         * This is the SAME repository and SAME Edge Function used
         * by AdvertAnalysisViewModel.
         */

        val analysis =
            advertAnalysisRepository
                .analyzeAdvert(

                    advertText =
                        text,

                    registration =
                        detectedRegistration,

                    officialVehicleData =
                        officialVehicleData
                )

        /*
         * ---------------------------------------------------------
         * STORE ANALYSIS
         * ---------------------------------------------------------

         */

        _advertAnalysis.value =
            analysis

        /*
         * ---------------------------------------------------------
         * RESULT SUMMARY
         * ---------------------------------------------------------
         */

        _result.value =
            ProVehicleSearchResult(

                summary =
                    "Pro advert analysis completed.",

                displayItems =
                    buildList {

                        detectedRegistration?.let {
                            add(
                                "Registration: $it"
                            )
                        }

                        if (
                            officialVehicleData != null
                        ) {
                            add(
                                "DVLA + DVSA/MOT"
                            )
                        }

                        add(
                            "Advert Analysis"
                        )

                        add(
                            "Pro vehicle analysis"
                        )
                    }
            )

        /*
         * ---------------------------------------------------------
         * NO REGISTRATION
         * ---------------------------------------------------------
         *
         * Advert analysis is still valid.
         *
         * There simply isn't an official vehicle payload to show.
         */

        if (
            detectedRegistration == null
        ) {

            _currentVehicle.value =
                null

            _currentRawJson.value =
                null

            _currentAnalysisResult.value =
                null

            _uiState.value =
                emptyList()
        }

        Log.d(
            "ProVehicleSearch",
            "Pro advert analysis completed."
        )
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE LOOKUP ONLY
     * =========================================================
     *
     * This method retrieves the official DVLA + DVSA/MOT payload.
     *
     * It does NOT perform any intelligence or analysis.
     */

    private suspend fun retrieveOfficialVehicleData(
        cleanPlate: String
    ): String {

        Log.d(
            "ProVehicleSearch",
            "Starting official lookup: $cleanPlate"
        )

        val responseData =
            officialLookup.performLookup(
                cleanPlate
            )

        /*
         * ---------------------------------------------------------
         * STORE RAW RESPONSE
         * ---------------------------------------------------------
         */

        _currentRawJson.value =
            responseData

        /*
         * ---------------------------------------------------------
         * PARSE VEHICLE
         * ---------------------------------------------------------
         */

        try {

            val vehicle =
                jsonParser
                    .decodeFromString<Vehicle>(
                        responseData
                    )

            _currentVehicle.value =
                vehicle

            /*
             * ---------------------------------------------------------
             * BUILD OFFICIAL VEHICLE UI DATA
             * ---------------------------------------------------------
             *
             * This replaces the old VehicleSearchReportBuilder
             * purely for presentation.
             *
             * No intelligence is generated here.
             */

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
        }

        Log.d(
            "ProVehicleSearch",
            "Official lookup completed: $cleanPlate"
        )

        return responseData
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE → EXISTING UI STATE
     * =========================================================
     *
     * This creates the List<Any> structure expected by the existing
     * ProVehicleSearchScreen / VehicleSearchSectionParser.
     *
     * It contains ONLY official vehicle information.
     *
     * It does NOT calculate intelligence.
     */

    private fun mapVehicleToUiState(
        vehicle: Vehicle
    ): List<Any> {

        val list =
            mutableListOf<Any>()

        /*
         * ---------------------------------------------------------
         * VEHICLE IDENTITY
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * TECHNICAL SPECIFICATIONS
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * REGISTRATION / DATES
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * TAX / MOT
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * OWNERSHIP / VEHICLE FLAGS
         * ---------------------------------------------------------
         */

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
                "Marked For Export" to
                    it.toString()
            )
        }

        /*
         * ---------------------------------------------------------
         * MOT HISTORY
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * ACTIVE SYMPTOMS
         * ---------------------------------------------------------
         */

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

            _rawAdvertInput.value =
                ""

            _selectedMotTest.value =
                null

            _saveMessage.value =
                null

            _isSaving.value =
                false
        }
    }
}
