package com.humraahi.ui.chat

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val isSentByMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap() = mapOf(
        "senderName" to senderName,
        "senderId" to if (isSentByMe) "local_user" else senderName,
        "text" to text,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ChatMessage {
            return ChatMessage(
                id = id,
                senderName = map["senderName"] as? String ?: "",
                text = map["text"] as? String ?: "",
                isSentByMe = (map["senderId"] as? String) == "local_user",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
