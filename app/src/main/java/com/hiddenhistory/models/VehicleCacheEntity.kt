package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VehicleCacheEntity(
    val id: String,
    val registration: String,

    @SerialName("dvla_data")
    val dvlaData: JsonObject? = null,

    @SerialName("dvsa_data")
    val dvsaData: JsonObject? = null,

    @SerialName("third_party_data")
    val thirdPartyData: JsonObject? = null,

    @SerialName("vehicle_summary")
    val vehicleSummary: JsonObject? = null,

    @SerialName("source_provider")
    val sourceProvider: String? = null,

    @SerialName("provider_report_id")
    val providerReportId: String? = null,

    @SerialName("first_searched_at")
    val firstSearchedAt: String,

    @SerialName("last_searched_at")
    val lastSearchedAt: String,

    @SerialName("last_third_party_update")
    val lastThirdPartyUpdate: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)
