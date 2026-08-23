package com.humraahi.ui.chat

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(tripId: String) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(application, tripId)
    )
    val messages by viewModel.messages.collectAsState()
    val sendError by viewModel.sendError.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteMessage by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(sendError) {
        sendError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSendError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isSentByMe = message.isSentBy(viewModel.currentUserId),
                        onLongPress = {
                            if (message.isSentBy(viewModel.currentUserId)) {
                                actionMessage = message
                            }
                        }
                    )
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

    actionMessage?.let { message ->
        MessageActionsDialog(
            onDismiss = { actionMessage = null },
            onEdit = {
                actionMessage = null
                editMessage = message
            },
            onDelete = {
                actionMessage = null
                deleteMessage = message
            }
        )
    }

    editMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editMessage = null },
            onSave = { updatedText ->
                viewModel.editMessage(message, updatedText)
                editMessage = null
            }
        )
    }

    deleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteMessage = null },
            title = { Text("Delete message?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(message)
                        deleteMessage = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isSentByMe: Boolean,
    onLongPress: () -> Unit
) {
    val bubbleColor = if (isSentByMe)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isSentByMe)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val alignment = if (isSentByMe) Alignment.End else Alignment.Start
    val bubbleShape = if (isSentByMe)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isSentByMe) {
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
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageActionsDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message options") },
        text = {
            Column {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditMessageDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(message.id) { mutableStateOf(message.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit message") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
