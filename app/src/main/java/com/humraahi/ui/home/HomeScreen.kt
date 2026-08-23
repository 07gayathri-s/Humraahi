package com.humraahi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import com.humraahi.model.Trip
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavController
import com.humraahi.navigation.Routes
import com.humraahi.ui.theme.humraahiTopAppBarColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncError) {
        syncError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncError()
        }
    }

    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Humraahi") },
                colors = humraahiTopAppBarColors(),
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {navController.navigate(Routes.NEW_TRIP)}) {
                Icon(Icons.Default.Add, contentDescription = "New Trip")
            }
        }
    ){ innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Success -> TripList(state.trips, Modifier.padding(innerPadding),navController)
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message)
                }
            }
        }
    }
}

@Composable
fun TripList(trips: List<Trip>, modifier: Modifier = Modifier, navController: NavController) {
    if (trips.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ){
        Text(
            text = "No trips yet. Start planning!"
        )
            }
    } else {
        LazyColumn(modifier = modifier) {
            items(trips) { trip ->
                TripCard(trip, navController = navController )
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { navController.navigate(Routes.tripDetail(trip.id)) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = trip.destination, style = MaterialTheme.typography.titleMedium)
            Text(text = "${trip.startDate} → ${trip.endDate}", style = MaterialTheme.typography.bodySmall)
        }
    }
}