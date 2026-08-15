package com.humraahi.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageTest {
    @Test
    fun toMapStoresStableSenderIdentity() {
        val message = ChatMessage(
            id = "message-1",
            senderId = "user-123",
            senderName = "Gayathri",
            text = "Let us visit Goa",
            timestamp = 1000L
        )

        val result = message.toMap()

        assertEquals("user-123", result["senderId"])
        assertEquals("Gayathri", result["senderName"])
        assertEquals("Let us visit Goa", result["text"])
        assertEquals(1000L, result["timestamp"])
    }

    @Test
    fun fromMapDerivesOwnershipForCurrentViewer() {
        val message = ChatMessage.fromMap(
            id = "message-1",
            map = mapOf(
                "senderId" to "user-123",
                "senderName" to "Gayathri",
                "text" to "Let us visit Goa",
                "timestamp" to 1000L
            )
        )

        assertTrue(message.isSentBy("user-123"))
        assertFalse(message.isSentBy("another-user"))
    }
}
