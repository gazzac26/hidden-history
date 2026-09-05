package com.hiddenhistory.models

import kotlinx.serialization.Serializable

@Serializable
data class HubMechanic(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: String,
    val distance: String,
    val isEmergencyAvailable: Boolean,
    val phone: String = "",         // Safe default for direct calling
    val latitude: Double = 0.0,     // Safe default for mapping
    val longitude: Double = 0.0     // Safe default for mapping
)