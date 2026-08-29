package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(

    @SerialName("id")
    val id: String,

    @SerialName("conversation_id")
    val conversationId: String,

    @SerialName("sender_id")
    val senderId: String,

    @SerialName("message_text")
    val messageText: String,

    @SerialName("message_type")
    val messageType: String = "text",

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("edited_at")
    val editedAt: String? = null,

    @SerialName("read_at")
    val readAt: String? = null,

    // ---------- UI ONLY ----------

    val isMine: Boolean = false,

    val senderName: String? = null,

    val senderAvatar: String? = null
)