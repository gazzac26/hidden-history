package com.hiddenhistory.viewmodel

import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.HiddenHistoryReportEntity
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow

class VehicleSearchState {

    /*
     * Current vehicle UI state.
     */
    val uiState =
        MutableStateFlow<List<Any>>(emptyList())

    /*
     * Loading state.
     */
    val isLoading =
        MutableStateFlow(false)

    /*
     * Currently selected MOT test.
     */
    val selectedMotTest =
        MutableStateFlow<MotTest?>(null)


    /*
     * Raw JSON for the currently displayed official vehicle.
     */
    val currentRawJson =
        MutableStateFlow<String?>(null)

    /*
     * Raw advert input.
     */
    val rawAdvertInput =
        MutableStateFlow("")

    /*
     * Result from the local advert engine.
     *
     * This is deliberately kept separate from Vehicle because
     * the advert is seller-provided evidence, whereas Vehicle
     * represents the official vehicle search result.
     */
    val parsedAdvert =
        MutableStateFlow<ParsedVehicleAdvert?>(null)

    /*
     * Vehicle mapped from advert data.
     */
    val mappedAdvertVehicle =
        MutableStateFlow<Vehicle?>(null)

    /*
     * Permanent Hidden History reports belonging to the
     * currently authenticated user.
     */
    val savedReports =
        MutableStateFlow<List<HiddenHistoryReportEntity>>(
            emptyList()
        )
}
