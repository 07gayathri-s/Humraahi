package com.humraahi.ui.tripdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.humraahi.data.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripDetailViewModel(
    private val tripId: String,
    private val repository: TripRepository = TripRepository()
) : ViewModel() {
    private val _joinState = MutableStateFlow<JoinTripState>(JoinTripState.Joining)
    val joinState: StateFlow<JoinTripState> = _joinState.asStateFlow()

    init {
        joinTrip()
    }

    fun retryJoin() {
        joinTrip()
    }

    private fun joinTrip() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            _joinState.value = JoinTripState.Error(
                "Sign in again before opening this trip."
            )
            return
        }

        viewModelScope.launch {
            _joinState.value = JoinTripState.Joining
            try {
                repository.joinTrip(tripId, userId)
                _joinState.value = JoinTripState.Ready
            } catch (error: FirebaseFirestoreException) {
                _joinState.value = JoinTripState.Error(
                    error.message ?: "This trip could not be opened."
                )
            }
        }
    }

    companion object {
        fun factory(tripId: String) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TripDetailViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return TripDetailViewModel(tripId) as T
            }
        }
    }
}

sealed class JoinTripState {
    object Joining : JoinTripState()
    object Ready : JoinTripState()
    data class Error(val message: String) : JoinTripState()
}
