package com.humraahi.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(tripId: String, viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(tripId))) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }

        MessageInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isSentByMe)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (message.isSentByMe)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val alignment = if (message.isSentByMe) Alignment.End else Alignment.Start
    val bubbleShape = if (message.isSentByMe)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!message.isSentByMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(color = bubbleColor, shape = bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Type a message…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
