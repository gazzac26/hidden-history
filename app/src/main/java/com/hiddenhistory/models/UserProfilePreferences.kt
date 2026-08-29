package com.hiddenhistory.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserProfilePreferences(
    @SerialName("preferences") val preferences: List<UserPreference> = emptyList()
)