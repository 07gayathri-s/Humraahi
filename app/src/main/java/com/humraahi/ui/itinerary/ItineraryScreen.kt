package com.humraahi.ui.itinerary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity

@Composable
fun ItineraryScreen(tripId: String) {
    val viewModel: ItineraryViewModel = viewModel(
        key = "itinerary-$tripId",
        factory = ItineraryViewModel.factory(tripId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var showAddActivity by rememberSaveable(tripId) { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        if (saveState is ItinerarySaveState.Success) {
            showAddActivity = false
            viewModel.resetSaveState()
        }
    }

    ItineraryContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onAddActivity = {
            viewModel.resetSaveState()
            showAddActivity = true
        }
    )

    if (showAddActivity) {
        AddActivityDialog(
            saveState = saveState,
            onDismiss = {
                showAddActivity = false
                viewModel.resetSaveState()
            },
            onSave = viewModel::saveActivity
        )
    }
}

@Composable
internal fun ItineraryContent(
    uiState: ItineraryUiState,
    onRetry: () -> Unit,
    onAddActivity: () -> Unit
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
                Button(onClick = onAddActivity) {
                    Text("Add activity")
                }
            }
        }
        is ItineraryUiState.Success -> {
            Column(modifier = Modifier.fillMaxSize()) {
                ItineraryDayList(uiState.itinerary, modifier = Modifier.weight(1f))
                Button(
                    onClick = onAddActivity,
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
private fun ItineraryDayList(itinerary: Itinerary, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itinerary.days.forEach { day ->
            item(key = "day-${day.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Day ${day.dayNumber}", style = MaterialTheme.typography.titleLarge)
                    day.date?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (day.title.isNotBlank()) {
                        Text(day.title, style = MaterialTheme.typography.titleMedium)
                    }
                    if (day.activities.isEmpty()) {
                        Text("No activities planned for this day.")
                    }
                }
            }
            items(day.activities, key = { "activity-${day.id}-${it.id}" }) { activity ->
                ActivityCard(activity)
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: ItineraryActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activity.startTime?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            Text(activity.title, style = MaterialTheme.typography.titleMedium)
            activity.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            activity.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
