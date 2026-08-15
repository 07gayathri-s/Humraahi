package com.humraahi.ui.newtrip

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humraahi.model.Trip
import com.humraahi.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(navController: NavController, viewModel: HomeViewModel) {
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    val isValid = destination.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()

    Scaffold(
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
            isValid = isValid,
            onDestinationChange = { destination = it },
            onStartDateChange = { startDate = it },
            onEndDateChange = { endDate = it },
            onCreateClick = {
                viewModel.addTrip(Trip(destination = destination, startDate = startDate, endDate = endDate))
                navController.popBackStack()
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
    isValid: Boolean,
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
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Trip")
        }
    }
}
