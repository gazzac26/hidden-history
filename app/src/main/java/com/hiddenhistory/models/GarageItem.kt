package com.hiddenhistory.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName

@OptIn(InternalSerializationApi::class)
@Serializable
data class GarageItem(
    @SerialName("id") // Ensure this matches your database column name
    val id: String, // Or Int, depending on your DB column type

    @SerialName("user_id")
    val userId: String,

    @SerialName("vehicle_registration")
    val vehicleRegistration: String,

    @SerialName("saved_at")
    val savedAt: String,

    @SerialName("vehicle_url")
    val vehicleUrl: String? = null,

    @SerialName("vehicle_make")
    val make: String? = null,

    @SerialName("vehicle_model")
    val model: String? = null,

    @SerialName("vehicle_year")
    val year: Int? = null,

    @SerialName("vehicle_mileage")
    val mileage: Int? = null,

    @SerialName("vehicle_price")
    val price: Double? = null,

    @SerialName("vehicle_description")
    val description: String? = null,

    @SerialName("vehicle_location")
    val location: String? = null,

    @SerialName("vehicle_contact_method")
    val contactMethod: String? = null,

    @SerialName("vehicle_image_urls")
    val imageUrls: List<String>? = null,

    @SerialName("vehicle_status")
    val status: String? = null,

    @SerialName("vehicle_seller_id")
    val sellerId: String? = null,

    @SerialName("vehicle_seller_name")
    val sellerName: String? = null,

    @SerialName("vehicle_seller_rating")
    val sellerRating: Double? = null,

    @SerialName("vehicle_seller_verified")
    val sellerVerified: Boolean? = null,

    @SerialName("vehicle_listing_created_at")
    val listingCreatedAt: String? = null,

    @SerialName("vehicle_transmission")
    val transmission: String? = null,

    @SerialName("vehicle_body_type")
    val bodyType: String? = null,

    @SerialName("vehicle_photo_url")
    val photoUrl: String? = null,
)