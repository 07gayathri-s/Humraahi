package com.humraahi.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.humraahi.model.Trip

@Composable
fun TripMembersDialog(
    trip: Trip?,
    currentUserId: String,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
    onRemoveMember: (String) -> Unit
) {
    var pendingRemoval by remember { mutableStateOf<MemberItem?>(null) }
    val members = trip?.memberIds.orEmpty().mapIndexed { index, memberId ->
        MemberItem(
            id = memberId,
            name = trip?.memberNames?.get(memberId)
                ?: trip?.members?.getOrNull(index)
                ?: "Traveller"
        )
    }
    val isOwner = trip?.createdBy == currentUserId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip members") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onInvite, modifier = Modifier.fillMaxWidth()) {
                    Text("Invite member")
                }
                if (members.isEmpty()) {
                    Text("Loading members…")
                } else {
                    LazyColumn {
                        items(members, key = { it.id }) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(member.name)
                                    when (member.id) {
                                        trip?.createdBy -> Text(
                                            "Owner",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        currentUserId -> Text(
                                            "You",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (isOwner && member.id != trip?.createdBy) {
                                    TextButton(onClick = { pendingRemoval = member }) {
                                        Text("Remove")
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    pendingRemoval?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${member.name}?") },
            text = { Text("They will lose access to this trip and its chat.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveMember(member.id)
                        pendingRemoval = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class MemberItem(
    val id: String,
    val name: String
)
