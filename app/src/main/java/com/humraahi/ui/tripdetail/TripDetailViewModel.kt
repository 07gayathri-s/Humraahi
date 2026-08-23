package com.humraahi.ui.tripdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.humraahi.data.TripRepository
import com.humraahi.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripDetailViewModel(
    application: Application,
    private val tripId: String,
    private val repository: TripRepository = TripRepository(application)
) : AndroidViewModel(application) {
    private val _joinState = MutableStateFlow<JoinTripState>(JoinTripState.Joining)
    val joinState: StateFlow<JoinTripState> = _joinState.asStateFlow()
    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()
    private val _memberError = MutableStateFlow<String?>(null)
    val memberError: StateFlow<String?> = _memberError.asStateFlow()
    val currentUserId: String = Firebase.auth.currentUser?.uid.orEmpty()

    init {
        joinTrip()
    }

    fun retryJoin() {
        joinTrip()
    }

    private fun joinTrip() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            _joinState.value = JoinTripState.Error(
                "Sign in again before opening this trip."
            )
            return
        }

        viewModelScope.launch {
            _joinState.value = JoinTripState.Joining
            try {
                repository.joinTrip(
                    tripId = tripId,
                    userId = user.uid,
                    userName = user.displayName ?: user.email ?: "Traveller"
                )
                _joinState.value = JoinTripState.Ready
                observeTrip()
            } catch (error: FirebaseFirestoreException) {
                _joinState.value = JoinTripState.Error(
                    error.message ?: "This trip could not be opened."
                )
            }
        }
    }

    private fun observeTrip() {
        viewModelScope.launch {
            try {
                repository.observeTrip(tripId).collect { _trip.value = it }
            } catch (error: Exception) {
                _memberError.value = error.message ?: "Members could not be loaded."
            }
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            try {
                repository.removeMember(tripId, memberId)
            } catch (error: Exception) {
                _memberError.value = error.message ?: "Member could not be removed."
            }
        }
    }

    fun clearMemberError() {
        _memberError.value = null
    }

    companion object {
        fun factory(
            application: Application,
            tripId: String
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TripDetailViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return TripDetailViewModel(application, tripId) as T
            }
        }
    }
}

sealed class JoinTripState {
    object Joining : JoinTripState()
    object Ready : JoinTripState()
    data class Error(val message: String) : JoinTripState()
}
