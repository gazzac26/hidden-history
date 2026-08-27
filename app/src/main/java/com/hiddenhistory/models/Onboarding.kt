package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Onboarding(
    @SerialName("id") val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("date_of_birth") val dateOfBirth: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("country_code") val countryCode: String,
    @SerialName("driving_confidence") val drivingConfidence: String,
    @SerialName("vehicle_knowledge") val vehicleKnowledge: String,
    @SerialName("onboarding_version") val onboardingVersion: Int,
    @SerialName("terms_accepted") val termsAccepted: Boolean,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
    @SerialName("marketing_consent") val marketingConsent: Boolean,
)
