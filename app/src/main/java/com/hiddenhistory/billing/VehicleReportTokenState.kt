package com.hiddenhistory.billing

sealed class VehicleReportTokenState {

    data object Idle :
        VehicleReportTokenState()

    data object Checking :
        VehicleReportTokenState()

    data object Reserving :
        VehicleReportTokenState()

    data class Reserved(
        val tokenId: String
    ) :
        VehicleReportTokenState()

    data class NoTokens(
        val availableCount: Int = 0
    ) :
        VehicleReportTokenState()

    data class Error(
        val message: String
    ) :
        VehicleReportTokenState()
}