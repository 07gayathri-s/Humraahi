package com.humraahi.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humraahi.data.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val tripId: String) : ViewModel() {
    private val repository = ChatRepository()

    val messages: StateFlow<List<ChatMessage>> = repository
        .getMessages(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderName = "Me",
                text = text,
                isSentByMe = true
            )
            repository.sendMessage(tripId, message)
        }
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
