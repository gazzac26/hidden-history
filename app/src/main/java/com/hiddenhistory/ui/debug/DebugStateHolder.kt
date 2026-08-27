package com.hiddenhistory.ui.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
object DebugStateHolder {

    /*
     * =========================================================
     * RAW PROVIDER DATA
     * =========================================================
     */

    var rawProviderData by mutableStateOf<Map<String, Any?>?>(null)
        private set


    /*
     * =========================================================
     * ADVERT ANALYSIS RESULT
     * =========================================================
     */

    var advertAnalysisResult by mutableStateOf<Any?>(null)
        private set

    /*
     * =========================================================
     * OFFICIAL ADVERT CROSS-CHECK RESULT
     * =========================================================
     *
     * This is deliberately stored separately from the advert
     * analysis because it is a separate stage of the pipeline.
     */

    var officialCrossCheckResult by mutableStateOf<Any?>(null)
        private set

    /*
     * =========================================================
     * VEHICLE SEARCH / ENGINE UPDATE
     * =========================================================
     */

    fun update(
        raw: Map<String, Any?>?
    ) {

        rawProviderData =
            raw
    }

    /*
     * =========================================================
     * ADVERT ANALYSIS UPDATE
     * =========================================================
     */

    fun updateAdvert(
        advertResult: Any?
    ) {

        advertAnalysisResult =
            advertResult
    }

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK UPDATE
     * =========================================================
     */

    fun updateOfficialCrossCheck(
        result: Any?
    ) {

        officialCrossCheckResult =
            result
    }

    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */

    fun clear() {

        rawProviderData =
            null

        advertAnalysisResult =
            null

        officialCrossCheckResult =
            null
    }
}