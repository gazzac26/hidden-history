package com.hiddenhistory.billing

sealed class VehicleReportPurchaseState {

    data object Idle : VehicleReportPurchaseState()

    data object Loading : VehicleReportPurchaseState()

    data class Available(
        val formattedPrice: String
    ) : VehicleReportPurchaseState()

    data object Purchasing : VehicleReportPurchaseState()

    /*
     * The purchase has completed successfully.
     *
     * The purchase token is retained because the next stage of
     * the system will use it when we connect the purchase to the
     * paid Gemini report entitlement.
     */
    data class Purchased(
        val purchaseToken: String
    ) : VehicleReportPurchaseState()

    /*
     * Google Play has accepted the transaction but payment has
     * not completed yet.
     */
    data object Pending : VehicleReportPurchaseState()

    data class Error(
        val message: String
    ) : VehicleReportPurchaseState()
}