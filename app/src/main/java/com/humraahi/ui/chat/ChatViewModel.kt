package com.humraahi.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.humraahi.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val tripId: String) : ViewModel() {
    private val repository = ChatRepository()
    private val auth: FirebaseAuth = Firebase.auth
    private val currentUser = auth.currentUser

    val currentUserId: String = currentUser?.uid.orEmpty()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository
        .getMessages(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(text: String) {
        val messageText = text.trim()
        if (messageText.isEmpty()) {
            _sendError.value = "Message cannot be empty."
            return
        }

        val user = currentUser
        if (user == null) {
            _sendError.value = "Sign in again before sending a message."
            return
        }

        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = user.uid,
                senderName = user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "Traveller",
                text = messageText
            )
            try {
                repository.sendMessage(tripId, message)
            } catch (error: FirebaseFirestoreException) {
                _sendError.value = error.message ?: "Message could not be sent."
            }
        }
    }

    fun clearSendError() {
        _sendError.value = null
    }

    companion object {
        fun factory(tripId: String) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(tripId) as T
            }
        }
    }
}
