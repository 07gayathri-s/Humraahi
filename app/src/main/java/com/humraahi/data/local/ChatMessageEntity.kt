package com.humraahi.data.local

import androidx.room.Entity
import com.humraahi.ui.chat.ChatMessage

@Entity(
    tableName = "chat_messages",
    primaryKeys = ["tripId", "id"]
)
data class ChatMessageEntity(
    val tripId: String,
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long
) {
    fun toChatMessage() = ChatMessage(
        id = id,
        senderId = senderId,
        senderName = senderName,
        text = text,
        timestamp = timestamp
    )

    companion object {
        fun fromChatMessage(tripId: String, message: ChatMessage) =
            ChatMessageEntity(
                tripId = tripId,
                id = message.id,
                senderId = message.senderId,
                senderName = message.senderName,
                text = message.text,
                timestamp = message.timestamp
            )
    }
}
