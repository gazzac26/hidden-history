package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(

    @SerialName("id")
    val id: String,

    @SerialName("user_1_id")
    val user1Id: String,

    @SerialName("user_2_id")
    val user2Id: String,

    @SerialName("created_at")
    val createdAt: String? = null
)