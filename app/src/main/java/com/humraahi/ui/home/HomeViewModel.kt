package com.humraahi.ui.home
import androidx.lifecycle.ViewModel
import com.humraahi.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Success(
            trips = listOf(
                Trip(destination = "Paris", startDate = "2024-07-01", endDate = "2024-07-10", members = listOf("Alice", "Bob")),
                Trip(destination = "New York", startDate = "2024-08-15", endDate = "2024-08-20", members = listOf("Charlie", "Dave"))
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    fun addTrip(trip: Trip) {
        val currentTrips = (_uiState.value as? HomeUiState.Success)?.trips ?: emptyList()
        _uiState.value = HomeUiState.Success(trips = currentTrips + trip)
    }
}