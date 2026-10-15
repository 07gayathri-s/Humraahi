package com.humraahi.ui.itinerary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.model.Trip
import com.humraahi.model.TripDateRange
import com.humraahi.util.DateFormatter

@Composable
fun ItineraryScreen(trip: Trip) {
    val datesResult = remember(trip.startDate, trip.endDate) {
        try {
            Result.success(TripDateRange(trip.startDate, trip.endDate))
        } catch (error: IllegalArgumentException) {
            Result.failure<TripDateRange>(error)
        }
    }
    val dateRange = datesResult.getOrNull()
    if (dateRange == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Trip dates are invalid: ${datesResult.exceptionOrNull()?.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    val viewModel: ItineraryViewModel = viewModel(
        key = "itinerary-${trip.id}",
        factory = ItineraryViewModel.factory(trip.id)
    )
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var showActivityDialog by rememberSaveable(trip.id) { mutableStateOf(false) }
    var editTarget by rememberSaveable(trip.id, stateSaver = ActivityEditTargetSaver) {
        mutableStateOf<ActivityEditTarget?>(null)
    }
    var deleteTarget by rememberSaveable(trip.id, stateSaver = ActivityEditTargetSaver) {
        mutableStateOf<ActivityEditTarget?>(null)
    }

    LaunchedEffect(saveState) {
        if (saveState is ItinerarySaveState.Success) {
            showActivityDialog = false
            editTarget = null
            deleteTarget = null
            viewModel.resetSaveState()
        }
    }

    ItineraryContent(
        uiState = uiState,
        dateRange = dateRange,
        actionsEnabled = saveState !is ItinerarySaveState.Saving,
        onRetry = viewModel::retry,
        onAddActivity = {
            viewModel.resetSaveState()
            editTarget = null
            showActivityDialog = true
        },
        onEditActivity = { target ->
            viewModel.resetSaveState()
            editTarget = target
            showActivityDialog = true
        },
        onDeleteActivity = { target ->
            viewModel.resetSaveState()
            deleteTarget = target
        }
    )

    if (showActivityDialog) {
        ActivityDialog(
            dateRange = dateRange,
            saveState = saveState,
            editTarget = editTarget,
            onDismiss = {
                showActivityDialog = false
                editTarget = null
                viewModel.resetSaveState()
            },
            onSave = { date, title, time, location, notes ->
                viewModel.saveActivity(date, title, time, location, notes, editTarget)
            }
        )
    }

    deleteTarget?.let { target ->
        DeleteActivityDialog(
            activity = target.activity,
            saveState = saveState,
            onDismiss = {
                deleteTarget = null
                viewModel.resetSaveState()
            },
            onConfirm = { viewModel.deleteActivity(target) }
        )
    }
}

@Composable
internal fun DeleteActivityDialog(
    activity: ItineraryActivity,
    saveState: ItinerarySaveState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDeleting = saveState is ItinerarySaveState.Saving
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Delete activity?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Delete \"${activity.title}\" for everyone? This cannot be undone. " +
                        "If this is the last activity on its date, that date section will also be removed."
                )
                if (saveState is ItinerarySaveState.Error) {
                    Text(saveState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(
                    if (isDeleting) "Deleting..." else "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun ItineraryContent(
    uiState: ItineraryUiState,
    dateRange: TripDateRange,
    onRetry: () -> Unit,
    onAddActivity: () -> Unit,
    onEditActivity: (ActivityEditTarget) -> Unit,
    onDeleteActivity: (ActivityEditTarget) -> Unit,
    actionsEnabled: Boolean = true
) {
    when (uiState) {
        ItineraryUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        ItineraryUiState.Empty -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No itinerary yet. Add your first activity to start planning.")
                Button(onClick = onAddActivity, enabled = actionsEnabled) {
                    Text("Add activity")
                }
            }
        }
        is ItineraryUiState.Success -> {
            Column(modifier = Modifier.fillMaxSize()) {
                ItineraryDayList(
                    uiState.itinerary, dateRange, onEditActivity, onDeleteActivity, actionsEnabled,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddActivity,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("Add activity")
                }
            }
        }
        is ItineraryUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun ItineraryDayList(
    itinerary: Itinerary,
    dateRange: TripDateRange,
    onEditActivity: (ActivityEditTarget) -> Unit,
    onDeleteActivity: (ActivityEditTarget) -> Unit,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itinerary.days.forEach { day ->
            val date = day.date ?: if (day.dayNumber in 1..dateRange.numberOfDays) {
                dateRange.dateForDay(day.dayNumber)
            } else null
            item(key = "day-${day.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        date?.let {
                            DateFormatter.formatForDisplay(DateFormatter.parseStorageDate(it))
                        } ?: "Day ${day.dayNumber} (outside trip dates)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (day.title.isNotBlank()) {
                        Text(day.title, style = MaterialTheme.typography.titleMedium)
                    }
                    if (day.activities.isEmpty()) {
                        Text("No activities planned for this day.")
                    }
                }
            }
            items(day.activities, key = { "activity-${day.id}-${it.id}" }) { activity ->
                ActivityCard(
                    activity,
                    actionsEnabled = actionsEnabled,
                    onEdit = { onEditActivity(ActivityEditTarget(day.id, date, activity)) },
                    onDelete = { onDeleteActivity(ActivityEditTarget(day.id, date, activity)) }
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ItineraryActivity,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activity.startTime?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            Text(activity.title, style = MaterialTheme.typography.titleMedium)
            activity.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            activity.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit, enabled = actionsEnabled) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete, enabled = actionsEnabled) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private val ActivityEditTargetSaver = mapSaver<ActivityEditTarget?>(
    save = { target ->
        if (target == null) emptyMap() else mapOf(
            "dayId" to target.dayId,
            "date" to target.date,
            "id" to target.activity.id,
            "title" to target.activity.title,
            "time" to target.activity.startTime,
            "location" to target.activity.location,
            "notes" to target.activity.notes
        )
    },
    restore = { values ->
        if (values.isEmpty()) null else ActivityEditTarget(
            dayId = values["dayId"] as String,
            date = values["date"] as String?,
            activity = ItineraryActivity(
                id = values["id"] as String,
                title = values["title"] as String,
                startTime = values["time"] as String?,
                location = values["location"] as String?,
                notes = values["notes"] as String?
            )
        )
    }
)
