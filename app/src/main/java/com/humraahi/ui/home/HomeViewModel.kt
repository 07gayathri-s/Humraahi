package com.humraahi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.humraahi.data.TripRepository
import com.humraahi.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = TripRepository()
    private val currentUser = Firebase.auth.currentUser

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _createTripState = MutableStateFlow<CreateTripState>(CreateTripState.Idle)
    val createTripState: StateFlow<CreateTripState> = _createTripState.asStateFlow()

    init {
        val userId = currentUser?.uid
        if (userId == null) {
            _uiState.value = HomeUiState.Error("Sign in again to load your trips.")
        } else {
            viewModelScope.launch {
                repository.observeTrips(userId)
                    .catch { error ->
                        _uiState.value = HomeUiState.Error(
                            error.message ?: "Trips could not be loaded."
                        )
                    }
                    .collect { trips ->
                        _uiState.value = HomeUiState.Success(trips)
                    }
            }
        }
    }

    fun createTrip(destination: String, startDate: String, endDate: String) {
        val user = currentUser
        if (user == null) {
            _createTripState.value = CreateTripState.Error(
                "Sign in again before creating a trip."
            )
            return
        }

        val trip = Trip(
            destination = destination.trim(),
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            members = listOfNotNull(user.displayName ?: user.email),
            memberIds = listOf(user.uid),
            createdBy = user.uid
        )

        viewModelScope.launch {
            _createTripState.value = CreateTripState.Saving
            try {
                repository.createTrip(trip)
                _createTripState.value = CreateTripState.Success
            } catch (error: FirebaseFirestoreException) {
                _createTripState.value = CreateTripState.Error(
                    error.message ?: "Trip could not be created."
                )
            }
        }
    }

    fun resetCreateTripState() {
        _createTripState.value = CreateTripState.Idle
    }
}

sealed class CreateTripState {
    object Idle : CreateTripState()
    object Saving : CreateTripState()
    object Success : CreateTripState()
    data class Error(val message: String) : CreateTripState()
}