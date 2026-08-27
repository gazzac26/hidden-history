package com.hiddenhistory.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class VehicleReport(
    val id: String? = null,
    val name: String? = null,
    val registration: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val mileage: Int? = null,
    val price: Double,
    val description: String? = null,
    val location: String? = null,
    val contact_method: String? = "",
    val image_urls: List<String>? = null,
    val status: String? = null,
    val seller_id: String? = null,
    val seller_name: String? = null,
    val seller_rating: Double? = null,
    val seller_verified: Boolean? = null,
    val listing_created_at: String? = null,
    val transmission: String? = null,
    val body_type: String? = null,
    val photo_url: String? = null,
    val photo_urls: List<String>?
)
