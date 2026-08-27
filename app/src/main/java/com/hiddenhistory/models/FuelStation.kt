package com.hiddenhistory.models

import kotlinx.serialization.Serializable

@Serializable
data class FuelStation(
    val id: String,
    val name: String,
    val distance: String,
    val unleadedPrice: String,
    val dieselPrice: String,
    val latitude: Double,
    val longitude: Double
)
