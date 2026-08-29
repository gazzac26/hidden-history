package com.hiddenhistory.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(InternalSerializationApi::class)
@Entity(tableName = "profiles")
@Serializable
data class UserProfile(
    @PrimaryKey
    @SerialName("id") val id: String = "",

    // --- Identity & Contact ---
    @SerialName("username") val username: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,

    // --- Address ---
    @SerialName("address_line_1") val addressLine1: String? = null,
    @SerialName("address_line_2") val addressLine2: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("county") val county: String? = null,
    @SerialName("postcode") val postcode: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,

    // --- Licence Info ---
    @SerialName("licence_number") val licenseNumber: String? = null,
    @SerialName("licence_type") val licenceType: String? = null,
    @SerialName("licence_pass_date") val licencePassDate: String? = null,

    // --- Bio & Images ---
    @SerialName("bio") val bio: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("occupation") val occupation: String? = null,
    @SerialName("raw_requirements") val rawRequirements: String? = null,

    // --- Onboarding & Preferences ---
    @SerialName("marketing_consent") val marketing_consent: Boolean? = false,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @SerialName("onboarding_version") val onboardingVersion: Int = 0,
    @SerialName("preferred_vehicle") val prefVehicle: String? = null,
    @SerialName("owned_vehicle") val ownedVehicle: String? = null,
    @SerialName("driving_confidence") val drivingConfidence: String? = null,
    @SerialName("vehicle_knowledge_level") val vehicleKnowledgeLevel: String? = null,
    @SerialName("primary_vehicle_use") val primaryVehicleUse: String? = null,

    // --- System Metadata ---
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_staff") val isStaff: Boolean = false,
    @SerialName("is_superuser") val isSuperuser: Boolean = false,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("is_email_verified") val isEmailVerified: Boolean = false,
    @SerialName("is_phone_verified") val isPhoneVerified: Boolean = false,
    @SerialName("is_sso_verified") val isSsoVerified: Boolean = false,
    @SerialName("is_mfa_enabled") val isMfaEnabled: Boolean = false,
    @SerialName("meets_attended") val meetsAttended: Int = 0,
    @SerialName("garage_level") val garageLevel: Int = 1,
    @SerialName("garage_progress") val garageProgress: Int = 0,
) {
    fun getMeetsCount(): Int {
        var count = 0
        if (!username.isNullOrEmpty()) count++
        if (!firstName.isNullOrEmpty()) count++
        if (!ownedVehicle.isNullOrEmpty()) count++
        return count
    }

    fun calculateGarageProgressPoints(): Int {
        var points = 0
        if (!username.isNullOrEmpty()) points += 10
        if (!firstName.isNullOrEmpty()) points += 10
        if (!ownedVehicle.isNullOrEmpty()) points += 20
        if (!licenseNumber.isNullOrEmpty()) points += 20
        if (!profileImageUrl.isNullOrEmpty()) points += 20
        if (onboardingCompleted) points += 20
        return points
    }

    fun getGarageLevelTitle(): String {
        val score = calculateGarageProgressPoints()
        return when {
            score >= 80 -> "Track Master"
            score >= 50 -> "Pro Tuner"
            score >= 20 -> "Garage Enthusiast"
            else -> "Track Novice"
        }
    }

    fun getFormattedAddress(): String {
        val parts = listOfNotNull(addressLine1, addressLine2, city, county, postcode).filter { it.isNotBlank() }
        return if (parts.isEmpty()) "No address on file" else parts.joinToString(", ")
    }
}