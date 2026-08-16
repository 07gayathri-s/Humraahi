package com.humraahi.data

import android.content.Context
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.humraahi.data.local.ChatMessageEntity
import com.humraahi.data.local.HumraahiDatabase
import com.humraahi.ui.chat.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatRepository(context: Context) {
    private val db = Firebase.firestore
    private val messageDao = HumraahiDatabase
        .getInstance(context)
        .chatMessageDao()

    private val _syncErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncErrors = _syncErrors.asSharedFlow()

    fun getMessages(tripId: String): Flow<List<ChatMessage>> = channelFlow {
        val syncJob = launch {
            observeRemoteMessages(tripId).collect { messages ->
                val entities = messages.map { message ->
                    ChatMessageEntity.fromChatMessage(tripId, message)
                }
                messageDao.replaceMessages(tripId, entities)
            }
        }

        try {
            messageDao.observeMessages(tripId)
                .map { entities ->
                    entities.map(ChatMessageEntity::toChatMessage)
                }
                .collect { messages ->
                    send(messages)
                }
        } finally {
            syncJob.cancel()
        }
    }

    private fun observeRemoteMessages(
        tripId: String
    ): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.collection("trips")
            .document(tripId)
            .collection("messages")
            .orderBy("timestamp")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _syncErrors.tryEmit(error.message ?: "Messages could not be synchronized.")
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
