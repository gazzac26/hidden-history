package com.hiddenhistory.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserPreference(
    @SerialName("category") val category: String,
    @SerialName("value") val value: String,
    @SerialName("weight") val weight: Int = 5
)