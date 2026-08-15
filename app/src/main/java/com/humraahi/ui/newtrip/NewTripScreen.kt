package com.humraahi.ui.newtrip

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humraahi.ui.home.CreateTripState
import com.humraahi.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(navController: NavController, viewModel: HomeViewModel) {
    val createTripState by viewModel.createTripState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    val isValid = destination.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()
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
            startDate = startDate,
            endDate = endDate,
            isCreateEnabled = isValid && !isSaving,
            isSaving = isSaving,
            onDestinationChange = { destination = it },
            onStartDateChange = { startDate = it },
            onEndDateChange = { endDate = it },
            onCreateClick = {
                viewModel.createTrip(destination, startDate, endDate)
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun TripForm(
    destination: String,
    startDate: String,
    endDate: String,
    isCreateEnabled: Boolean,
    isSaving: Boolean,
    onDestinationChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
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
        OutlinedTextField(
            value = startDate,
            onValueChange = onStartDateChange,
            label = { Text("Start date") },
            placeholder = { Text("e.g. Dec 20") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = endDate,
            onValueChange = onEndDateChange,
            label = { Text("End date") },
            placeholder = { Text("e.g. Dec 25") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
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
