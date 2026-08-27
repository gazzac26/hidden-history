package com.hiddenhistory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationSummary(

    @SerialName("conversation_id")
    val conversationId: String,

    @SerialName("other_user_id")
    val otherUserId: String,

    @SerialName("other_username")
    val otherUsername: String? = null,

    @SerialName("other_avatar")
    val otherAvatar: String? = null,

    @SerialName("last_message")
    val lastMessage: String? = null,

    @SerialName("last_message_type")
    val lastMessageType: String? = "text",

    @SerialName("last_message_at")
    val lastMessageAt: String? = null,

    @SerialName("unread_count")
    val unreadCount: Int = 0

)