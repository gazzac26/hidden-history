package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HiddenHistoryReportInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("vehicle_cache_id")
    val vehicleCacheId: String? = null,

    val registration: String,

    @SerialName("report_type")
    val reportType: String = "FULL_VEHICLE_REPORT",

    @SerialName("vehicle_data")
    val vehicleData: JsonObject? = null,

    @SerialName("dvla_data")
    val dvlaData: JsonObject? = null,

    @SerialName("dvsa_data")
    val dvsaData: JsonObject? = null,

    @SerialName("third_party_data")
    val thirdPartyData: JsonObject? = null,

    @SerialName("advert_data")
    val advertData: JsonObject? = null,

    @SerialName("language_analysis")
    val languageAnalysis: JsonObject? = null,

    @SerialName("report_summary")
    val reportSummary: String? = null,

    @SerialName("provider_name")
    val providerName: String? = null,

    @SerialName("provider_report_id")
    val providerReportId: String? = null
)
