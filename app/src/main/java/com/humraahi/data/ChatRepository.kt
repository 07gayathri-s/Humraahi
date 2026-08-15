package com.humraahi.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.humraahi.ui.chat.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = Firebase.firestore

    fun getMessages(tripId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.collection("trips")
            .document(tripId)
            .collection("messages")
            .orderBy("timestamp")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.let { ChatMessage.fromMap(doc.id, it) }
            } ?: emptyList()
            trySend(messages)
        }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(tripId: String, message: ChatMessage) {
        db.collection("trips")
            .document(tripId)
            .collection("messages")
            .document(message.id)
            .set(message.toMap())
            .await()
    }
}
