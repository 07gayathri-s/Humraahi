package com.humraahi.data.local

import com.humraahi.ui.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageEntityTest {
    @Test
    fun entityRoundTripPreservesMessageData() {
        val message = ChatMessage(
            id = "message-1",
            senderId = "user-1",
            senderName = "Gayathri",
            text = "Let us visit Goa",
            timestamp = 1000L
        )

        val entity = ChatMessageEntity.fromChatMessage("trip-1", message)

        assertEquals("trip-1", entity.tripId)
        assertEquals(message, entity.toChatMessage())
    }
}
