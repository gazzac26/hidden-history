package com.hiddenhistory.models

import kotlinx.serialization.Serializable

@Serializable
data class AdvertAnalysis(
    val registrationNumber: String? = null,
    val advertTitle: String? = null,
    val sellerInformation: String? = null,
    val vehicleDetails: Map<String, String>? = null,
    val price: String? = null,
    val mileage: String? = null,
    val claimsMadeBySeller: List<String> = emptyList(),
    val notableWording: List<String> = emptyList(),
    val missingInformation: List<String> = emptyList(),
    val inconsistencies: List<String> = emptyList(),
    val thingsWorthVerifying: List<String> = emptyList(),
    val overallSummary: String? = null,
    val questionsTheBuyerShouldAsk: List<String> = emptyList()
)
