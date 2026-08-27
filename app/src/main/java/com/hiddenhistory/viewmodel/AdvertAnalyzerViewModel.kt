package com.hiddenhistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.engine.AdvertParserEngine
import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.ui.debug.DebugStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdvertAnalyzerViewModel : ViewModel() {

    /*
     * =========================================================
     * FREE ADVERT ANALYSIS ENGINE
     * =========================================================
     *
     * This is the existing deterministic advert-analysis engine.
     *
     * It analyses what the SELLER has written.
     */
    private val parser =
        AdvertParserEngine()

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK ENGINE
     * =========================================================
     *
     * This compares what the seller said against official
     * vehicle/MOT information returned by Vehicle Search.
     */
    private val crossCheckEngine =
        AdvertOfficialCrossCheckEngine()

    /*
     * =========================================================
     * PARSED ADVERT
     * =========================================================
     *
     * This remains the main free advert-analysis result.
     */
    private val _parsedResult =
        MutableStateFlow<ParsedVehicleAdvert?>(null)

    val parsedResult:
        StateFlow<ParsedVehicleAdvert?> =
        _parsedResult.asStateFlow()

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK RESULT
     * =========================================================
     */
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
     * ANALYSE ADVERT
     * =========================================================
     */
    fun analyzeAdvert(
        rawText: String
    ) {

        viewModelScope.launch {

            if (rawText.isBlank()) {
                return@launch
            }

            /*
             * Parse the advert.
             */
            val result =
                parser.parse(
                    rawText
                )

            /*
             * Store the advert analysis.
             */
            _parsedResult.value =
                result

            /*
             * Send the parsed result to the debug inspector holder
             */
            DebugStateHolder.updateAdvert(result)

            /*
             * Official cross-check is reset here because the
             * new advert has not yet been compared against
             * official vehicle data.
             */
            _officialCrossCheck.value =
                null
        }
    }

    /*
     * =========================================================
     * CROSS-CHECK AGAINST OFFICIAL MOT DATA
     * =========================================================
     */
    fun crossCheckAgainstOfficialMot(
        motTests: List<MotTest>
    ) {

        viewModelScope.launch {

            val advert =
                _parsedResult.value
                    ?: return@launch

            val result =
                crossCheckEngine.compare(
                    advert = advert,
                    motTests = motTests
                )

            _officialCrossCheck.value =
                result
        }
    }

    /*
     * =========================================================
     * CLEAR ANALYSIS
     * =========================================================
     */
    fun clearAnalysis() {

        _parsedResult.value =
            null

        _officialCrossCheck.value =
            null

        DebugStateHolder.updateAdvert(null)
    }
}
