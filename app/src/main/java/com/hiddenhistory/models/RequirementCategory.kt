package com.hiddenhistory.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Entity(tableName = "saved_vehicles")
@Serializable
data class RequirementCategory (
    @PrimaryKey
    @SerialName("id") val id: String = "",
    // --- Existing Fields (Preserved Exactly) ---
    @SerialName("family") val family: String? = null,
    @SerialName("budget") val budget: String? = null,
    @SerialName("performance") val performance: String? = null,
    @SerialName("economy") val economy: String? = null,
    @SerialName("insurance") val insurance: String? = null,
    @SerialName("storage") val storage: String? = null,
    @SerialName("commuting") val commuting: String? = null,
    @SerialName("towing") val towing: String? = null,
    @SerialName("business") val business: String? = null,
    @SerialName("accessibility") val accessibility: String? = null,
    @SerialName("fuel") val fuel: String? = null,
    @SerialName("transmission") val transmission: String? = null,
    @SerialName("custom") val custom: String? = null,
    @SerialName("raw_requirements") val rawRequirements: String? = null,

    // --- Lifestyle & Environmental Additions ---
    @SerialName("MILEAGE") val MILEAGE: String? = null,
    @SerialName("ENVIRONMENT") val ENVIRONMENT: String? = null,
    @SerialName("PETS") val PETS: String? = null,
    @SerialName("LIFESTYLE") val LIFESTYLE: String? = null,
    @SerialName("SAFETY") val SAFETY: String? = null,
    @SerialName("TERRAIN") val TERRAIN: String? = null,
    @SerialName("TECHNOLOGY") val TECHNOLOGY: String? = null,
    @SerialName("COMFORT") val COMFORT: String? = null,
    @SerialName("RELIABILITY") val RELIABILITY: String? = null,

    // --- EV, Battery, & AI Future-proofing Additions ---
    @SerialName("RANGE") val RANGE: String? = null,
    @SerialName("CHARGING") val CHARGING: String? = null,
    @SerialName("AI_FEATURES") val AI_FEATURES: String? = null
)

{
    fun getMeetsCount(): Int {
        var count = 0
        if (!budget.isNullOrEmpty()) count++
        if (!performance.isNullOrEmpty()) count++
        if (!economy.isNullOrEmpty()) count++
        if (!insurance.isNullOrEmpty()) count++
        if (!storage.isNullOrEmpty()) count++
        if (!commuting.isNullOrEmpty()) count++
        if (!towing.isNullOrEmpty()) count++
        if (!business.isNullOrEmpty()) count++
        if (!accessibility.isNullOrEmpty()) count++
        if (!fuel.isNullOrEmpty()) count++
        if (!transmission.isNullOrEmpty()) count++
        if (!custom.isNullOrEmpty()) count++
        if (!family.isNullOrEmpty()) count++
        return count
    }
}