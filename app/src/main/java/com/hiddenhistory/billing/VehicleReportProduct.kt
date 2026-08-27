package com.hiddenhistory.billing

object VehicleReportProduct {

    /*
     * Google Play Console product ID.
     *
     * This is deliberately kept in one place so the product ID
     * does not get duplicated throughout the application.
     *
     * This is a ONE-TIME CONSUMABLE product.
     *
     * It represents one paid AI vehicle report.
     */
    const val PRODUCT_ID = "vehicle_full_ai_report"

    /*
     * Product type used by Google Play Billing.
     */
    const val PRODUCT_TYPE = "inapp"
}