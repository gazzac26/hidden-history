package com.hiddenhistory.models

import kotlinx.serialization.Serializable

@Serializable
data class MarketListing(
    val id: Long,
    val user_id: String,
    val title: String,
    val description: String?,
    val price: Double,
    val condition: String?,
    val image_url: String?,
    val is_active: Boolean
)