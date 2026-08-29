package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
     * Pro advert analysis is provided by AdvertAnalysis.
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
     * Advert pipeline:
     *
     * Advert input
     *      ↓
     * AdvertParserEngine
     *      ↓
     * Extract advert information
     *      ↓
     * smooth-handler / official lookup
     *      ↓
     * Official vehicle JSON
     *      ↓
     * AdvertAnalysisRepository
     *      ↓
     * analyse-advert Edge Function
     *      ↓
     * Gemini
     *      ↓
     * AdvertAnalysis
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
     * SMOOTH HANDLER / OFFICIAL VEHICLE LOOKUP
     * =========================================================
     *
     * VehicleSearchOfficialLookup is the client-side wrapper
     * around the official vehicle lookup Edge Function.
     *
     * That Edge Function is the smooth-handler:
     *
     *     registration
     *          ↓
     *     DVLA + DVSA
     *          ↓
     *     canonical vehicle JSON
     *
     * It does NOT perform Gemini analysis.
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
     * This is the ONLY analysis hand-off.
     *
     * AdvertAnalysisRepository sends:
     *
     * - complete advert text
     * - extracted registration
     * - official vehicle JSON
     *
     * to analyse-advert.
     *
     * The Edge Function then handles Gemini.
     */

    private val advertAnalysisRepository =
        AdvertAnalysisRepository(
            SupabaseManager.client
        )

    /*
     * =========================================================
     * ADVERT INFORMATION EXTRACTION
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
     * REGISTRATION EXTRACTION
     * =========================================================
     *
     * Detects modern UK registrations inside a full advert.
     *
     * Examples:
     *
     * BV68LDF
     * BV68 LDF
     * AB12 CDE
     */

    private val ukModernRegPattern =
        Regex(
            pattern =
                """(?<![A-Z0-9])([A-Z]{2}[0-9]{2}\s?[A-Z]{3})(?![A-Z0-9])""",

            options =
                setOf(
                    RegexOption.IGNORE_CASE
                )
        )

    private fun extractRegistration(
        text: String
    ): String? {

        val match =
            ukModernRegPattern.find(
                text
            )

        if (
            match == null
        ) {

            Log.d(
                "ProVehicleSearch",
                "No modern UK registration found in advert."
            )

            return null
        }

        val candidate =
            match
                .groupValues[1]
                .uppercase()
                .replace(
                    Regex("\\s+"),
                    ""
                )

        Log.d(
            "ProVehicleSearch",
            "Registration extracted from advert: $candidate"
        )

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

                /*
                 * Re-run the existing advert parser only for
                 * persistence / cross-check data.
                 *
                 * This does NOT trigger another AI analysis.
                 */

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
                                advert =
                                    advert,

                                vehicle =
                                    vehicle
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
     * REGISTRATION ONLY:
     *
     *     Registration
     *          ↓
     *     smooth-handler
     *          ↓
     *     Official vehicle data
     *          ↓
     *     Result
     *
     *
     * FULL ADVERT:
     *
     *     Advert
     *          ↓
     *     AdvertParserEngine
     *          ↓
     *     Advert information
     *          ↓
     *     Extract registration
     *          ↓
     *     smooth-handler
     *          ↓
     *     Official vehicle data
     *          ↓
     *     analyse-advert
     *          ↓
     *     Gemini
     *          ↓
     *     AdvertAnalysis
     *          ↓
     *     Result
     *
     *
     * FULL ADVERT WITHOUT REGISTRATION:
     *
     *     Advert
     *          ↓
     *     AdvertParserEngine
     *          ↓
     *     Advert information
     *          ↓
     *     analyse-advert
     *          ↓
     *     Gemini
     *          ↓
     *     AdvertAnalysis
     *          ↓
     *     Result
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
                    trimmed
                        .replace(
                            Regex("\\s+"),
                            ""
                        )
                        .uppercase()

                /*
                 * -------------------------------------------------
                 * STRICT MODERN UK STANDALONE REGISTRATION
                 * -------------------------------------------------
                 */

                val strictUkPlateRegex =
                    Regex(
                        pattern =
                            "^[A-Z]{2}[0-9]{2}[A-Z]{3}$",

                        options =
                            setOf(
                                RegexOption.IGNORE_CASE
                            )
                    )

                val isStandaloneRegistration =
                    strictUkPlateRegex.matches(
                        compactInput
                    )

                Log.d(
                    "ProVehicleSearch",
                    "Input compacted: $compactInput"
                )

                Log.d(
                    "ProVehicleSearch",
                    "Standalone registration: $isStandaloneRegistration"
                )

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
                 */

                if (
                    isStandaloneRegistration
                ) {

                    processRegistrationInputInternal(
                        compactInput
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
         * SMOOTH HANDLER
         * ---------------------------------------------------------
         *
         * This is the official vehicle data stage.
         *
         * The handler:
         *
         * - authenticates the user
         * - checks Pro entitlement
         * - calls DVLA
         * - calls DVSA
         * - merges the results
         * - returns canonical vehicle JSON
         *
         * It does NOT call Gemini.
         */

        val officialVehicleData =
            retrieveOfficialVehicleData(
                cleanPlate
            )

        /*
         * ---------------------------------------------------------
         * CLEAR ADVERT ANALYSIS
         * ---------------------------------------------------------
         */

        _advertAnalysis.value =
            null

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
     * THIS IS THE IMPORTANT PIPELINE CHANGE.
     *
     * The advert is no longer treated as simply:
     *
     *     advert → registration → analysis
     *
     * Instead:
     *
     *     advert
     *       ↓
     *     AdvertParserEngine
     *       ↓
     *     extracted advert information
     *       ↓
     *     registration
     *       ↓
     *     smooth-handler
     *       ↓
     *     official vehicle data
     *       ↓
     *     analyse-advert
     *       ↓
     *     Gemini
     *
     * The COMPLETE original advert is still sent to
     * AdvertAnalysisRepository.
     *
     * The official vehicle JSON is also supplied when available.
     */

    private suspend fun processAdvertInputInternal(
        text: String
    ) {

        Log.d(
            "ProVehicleSearch",
            "Starting Pro advert pipeline."
        )

        /*
         * ---------------------------------------------------------
         * STAGE 1 — EXTRACT ADVERT INFORMATION
         * ---------------------------------------------------------
         *
         * Use the existing AdvertParserEngine as the first
         * processing stage.
         *
         * This gives the pipeline a structured advert object
         * before official data retrieval / AI analysis.
         */

        val parsedAdvert =
            try {

                advertParser
                    .parse(text)

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

        if (
            parsedAdvert != null
        ) {

            Log.d(
                "ProVehicleSearch",
                "Advert information extracted successfully."
            )

        } else {

            Log.d(
                "ProVehicleSearch",
                "Advert parser returned no structured result. Continuing with raw advert."
            )
        }

        /*
         * ---------------------------------------------------------
         * STAGE 2 — EXTRACT REGISTRATION
         * ---------------------------------------------------------
         *
         * Keep the existing robust registration extraction.
         *
         * This allows the pipeline to work even if the parser
         * does not expose registration as a dedicated property.
         */

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
         * STAGE 3 — SMOOTH HANDLER
         * ---------------------------------------------------------
         *
         * If a registration exists, the smooth-handler retrieves
         * the official DVLA + DVSA/MOT record.
         *
         * IMPORTANT:
         *
         * There is NO Gemini call here.
         *
         * This stage exists solely to build the official vehicle
         * data that analyse-advert will subsequently receive.
         */

        val officialVehicleData =
            if (
                detectedRegistration != null
            ) {

                Log.d(
                    "ProVehicleSearch",
                    "Registration found."
                )

                Log.d(
                    "ProVehicleSearch",
                    "Passing registration to smooth-handler: $detectedRegistration"
                )

                retrieveOfficialVehicleData(
                    detectedRegistration
                )

            } else {

                Log.d(
                    "ProVehicleSearch",
                    "No registration found."
                )

                Log.d(
                    "ProVehicleSearch",
                    "Smooth-handler skipped because no registration is available."
                )

                _currentVehicle.value =
                    null

                _currentRawJson.value =
                    null

                null
            }

        /*
         * ---------------------------------------------------------
         * STAGE 4 — ANALYSE ADVERT
         * ---------------------------------------------------------
         *
         * This is now deliberately AFTER the smooth-handler.
         *
         * The analysis repository receives:
         *
         * 1. COMPLETE advert text
         * 2. extracted registration
         * 3. official DVLA/DVSA vehicle JSON
         *
         * The repository then invokes:
         *
         *     analyse-advert Edge Function
         *
         * The Edge Function connects to Gemini.
         */

        Log.d(
            "ProVehicleSearch",
            "Official data retrieval stage complete."
        )

        Log.d(
            "ProVehicleSearch",
            "Official vehicle data available: ${
                officialVehicleData != null
            }"
        )

        Log.d(
            "ProVehicleSearch",
            "Now passing advert + official vehicle data to analyse-advert."
        )

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
         * STAGE 5 — STORE FULL ANALYSIS
         * ---------------------------------------------------------
         */

        _advertAnalysis.value =
            analysis

        Log.d(
            "ProVehicleSearch",
            "analyse-advert completed successfully."
        )

        /*
         * ---------------------------------------------------------
         * STAGE 6 — RESULT SUMMARY
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
         * The advert still goes through analyse-advert.
         *
         * Gemini therefore receives the advert even when an
         * official vehicle record cannot be obtained.
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

            Log.d(
                "ProVehicleSearch",
                "Advert had no registration. Analysis completed using advert information only."
            )
        }

        Log.d(
            "ProVehicleSearch",
            "Pro advert pipeline completed."
        )
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE LOOKUP / SMOOTH HANDLER
     * =========================================================
     *
     * This method is intentionally isolated from advert analysis.
     *
     * It ONLY:
     *
     *     registration
     *          ↓
     *     smooth-handler
     *          ↓
     *     DVLA + DVSA
     *          ↓
     *     canonical JSON
     *
     * No AI analysis happens here.
     */

    private suspend fun retrieveOfficialVehicleData(
        cleanPlate: String
    ): String {

        Log.d(
            "ProVehicleSearch",
            "Starting smooth-handler official lookup: $cleanPlate"
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
            "Smooth-handler official lookup completed: $cleanPlate"
        )

        return responseData
    }

    /*
     * =========================================================
     * OFFICIAL VEHICLE → EXISTING UI STATE
     * =========================================================
     *
     * ONLY official vehicle information is mapped here.
     *
     * No intelligence is generated.
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
                "Marked for Export" to
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