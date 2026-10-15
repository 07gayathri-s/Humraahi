package com.humraahi.ui.itinerary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.humraahi.model.TripDateRange
import com.humraahi.util.DateFormatter

@Composable
internal fun ActivityDialog(
    dateRange: TripDateRange,
    saveState: ItinerarySaveState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    editTarget: ActivityEditTarget? = null
) {
    val activity = editTarget?.activity
    var selectedDate by rememberSaveable(activity?.id) {
        mutableStateOf(if (editTarget == null) dateRange.startDate else editTarget.date)
    }
    var showDatePicker by rememberSaveable(activity?.id) { mutableStateOf(false) }
    var title by rememberSaveable(activity?.id) { mutableStateOf(activity?.title.orEmpty()) }
    var startTime by rememberSaveable(activity?.id) { mutableStateOf(activity?.startTime.orEmpty()) }
    var location by rememberSaveable(activity?.id) { mutableStateOf(activity?.location.orEmpty()) }
    var notes by rememberSaveable(activity?.id) { mutableStateOf(activity?.notes.orEmpty()) }
    val isSaving = saveState is ItinerarySaveState.Saving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (editTarget == null) "Add activity" else "Edit activity") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate?.let {
                            DateFormatter.formatForDisplay(DateFormatter.parseStorageDate(it))
                        }.orEmpty(),
                        onValueChange = {},
                        label = { Text("Date") },
                        placeholder = { Text("Select date") },
                        readOnly = true,
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier.matchParentSize()
                            .clickable(enabled = !isSaving) { showDatePicker = true }
                            .semantics { contentDescription = "Choose activity date" }
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Time (optional)") },
                    placeholder = { Text("18:00") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 3,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                if (saveState is ItinerarySaveState.Error) {
                    Text(saveState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedDate.orEmpty(), title, startTime, location, notes) },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker && !isSaving) {
        ActivityDatePicker(
            dateRange = dateRange,
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDatePicker(
    dateRange: TripDateRange,
    selectedDate: String?,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let(DateFormatter::parseStorageDate)
            ?.takeIf { it in dateRange.startMillis..dateRange.endMillis },
        initialDisplayedMonthMillis = dateRange.startMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in dateRange.startMillis..dateRange.endMillis

            override fun isSelectableYear(year: Int): Boolean =
                year in dateRange.startDate.take(4).toInt()..dateRange.endDate.take(4).toInt()
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let {
                        onDateSelected(DateFormatter.formatForStorage(it))
                    }
                }
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}
