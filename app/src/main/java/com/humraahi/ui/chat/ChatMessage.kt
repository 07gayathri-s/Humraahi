package com.humraahi.ui.chat

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap() = mapOf(
        "senderName" to senderName,
        "senderId" to senderId,
        "text" to text,
        "timestamp" to timestamp
    )

    fun isSentBy(userId: String): Boolean = senderId == userId

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ChatMessage {
            return ChatMessage(
                id = id,
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                text = map["text"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
