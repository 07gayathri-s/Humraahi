package com.humraahi.ui.newtrip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humraahi.ui.home.CreateTripState
import com.humraahi.ui.home.HomeViewModel
import com.humraahi.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(navController: NavController, viewModel: HomeViewModel) {
    val createTripState by viewModel.createTripState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by remember { mutableStateOf("") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val hasInvalidDateRange = startDateMillis?.let { startDate ->
        endDateMillis?.let { endDate -> endDate < startDate }
    } ?: false
    val isValid = destination.isNotBlank()
        && startDateMillis != null
        && endDateMillis != null
        && !hasInvalidDateRange
    val isSaving = createTripState is CreateTripState.Saving

    LaunchedEffect(createTripState) {
        when (val state = createTripState) {
            CreateTripState.Success -> {
                viewModel.resetCreateTripState()
                navController.popBackStack()
            }
            is CreateTripState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetCreateTripState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Trip") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        TripForm(
            destination = destination,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            hasInvalidDateRange = hasInvalidDateRange,
            isCreateEnabled = isValid && !isSaving,
            isSaving = isSaving,
            onDestinationChange = { destination = it },
            onStartDateClick = { showStartDatePicker = true },
            onEndDateClick = { showEndDatePicker = true },
            onCreateClick = {
                val startDate = startDateMillis
                val endDate = endDateMillis
                if (startDate != null && endDate != null) {
                    viewModel.createTrip(
                        destination = destination,
                        startDate = DateFormatter.formatForStorage(startDate),
                        endDate = DateFormatter.formatForStorage(endDate)
                    )
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showStartDatePicker) {
        TripDatePickerDialog(
            initialDateMillis = startDateMillis,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { selectedDate ->
                startDateMillis = selectedDate
                if (endDateMillis?.let { it < selectedDate } == true) {
                    endDateMillis = null
                }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        TripDatePickerDialog(
            initialDateMillis = endDateMillis ?: startDateMillis,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { selectedDate ->
                endDateMillis = selectedDate
                showEndDatePicker = false
            }
        )
    }
}

@Composable
fun TripForm(
    destination: String,
    startDateMillis: Long?,
    endDateMillis: Long?,
    hasInvalidDateRange: Boolean,
    isCreateEnabled: Boolean,
    isSaving: Boolean,
    onDestinationChange: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = destination,
            onValueChange = onDestinationChange,
            label = { Text("Destination") },
            placeholder = { Text("e.g. Goa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DatePickerField(
            label = { Text("Start date") },
            selectedDateMillis = startDateMillis,
            onClick = onStartDateClick
        )
        DatePickerField(
            label = { Text("End date") },
            selectedDateMillis = endDateMillis,
            onClick = onEndDateClick,
            isError = hasInvalidDateRange
        )
        if (hasInvalidDateRange) {
            Text(
                text = "End date must be on or after the start date.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = onCreateClick,
            enabled = isCreateEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Trip")
            }
        }
    }
}

@Composable
private fun DatePickerField(
    label: @Composable () -> Unit,
    selectedDateMillis: Long?,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedDateMillis?.let(DateFormatter::formatForDisplay).orEmpty(),
            onValueChange = {},
            label = label,
            placeholder = { Text("Select date") },
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null)
            },
            isError = isError,
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
