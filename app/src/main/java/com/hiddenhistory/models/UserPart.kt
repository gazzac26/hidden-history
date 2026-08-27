package com.hiddenhistory.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPart(
    val id: Int,
    val part_name: String, // Ensure this matches your DB column name
    val price: Double,
    val user_id: String
)