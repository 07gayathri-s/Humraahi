package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humraahi.data.ItineraryRepository
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.util.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ItineraryViewModel(
    private val itineraryUpdates: Flow<Itinerary?>,
    private val addActivity: suspend (String, ItineraryActivity) -> Unit,
    private val updateActivity: suspend (ActivityEditTarget, String, ItineraryActivity) -> Unit
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Loading)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<ItinerarySaveState>(ItinerarySaveState.Idle)
    val saveState: StateFlow<ItinerarySaveState> = _saveState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeItinerary()
    }

    fun retry() {
        observeItinerary()
    }

    fun saveActivity(
        date: String,
        title: String,
        startTime: String,
        location: String,
        notes: String,
        editTarget: ActivityEditTarget? = null
    ) {
        if (_saveState.value is ItinerarySaveState.Saving) return

        try {
            DateFormatter.parseStorageDate(date)
        } catch (error: IllegalArgumentException) {
            _saveState.value = ItinerarySaveState.Error("Choose a valid activity date.")
            return
        }
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _saveState.value = ItinerarySaveState.Error("Activity name cannot be empty.")
            return
        }
        val time = startTime.trim().takeIf { it.isNotEmpty() }
        if (time != null && !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(time)) {
            _saveState.value = ItinerarySaveState.Error(
                "Enter time in 24-hour HH:mm format, for example 18:00."
            )
            return
        }

        val details = ItineraryActivity(
            title = trimmedTitle,
            startTime = time,
            location = location.trim().takeIf { it.isNotEmpty() },
            notes = notes.trim().takeIf { it.isNotEmpty() }
        )
        val activity = editTarget?.activity?.copy(
            title = details.title,
            startTime = details.startTime,
            location = details.location,
            notes = details.notes
        ) ?: details
        _saveState.value = ItinerarySaveState.Saving
        viewModelScope.launch {
            try {
                if (editTarget == null) {
                    addActivity(date, activity)
                } else {
                    updateActivity(editTarget, date, activity)
                }
                _saveState.value = ItinerarySaveState.Success
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _saveState.value = ItinerarySaveState.Error(
                    error.message ?: "Activity could not be saved. Please try again."
                )
            }
        }
    }

    fun resetSaveState() {
        if (_saveState.value !is ItinerarySaveState.Saving) {
            _saveState.value = ItinerarySaveState.Idle
        }
    }

    private fun observeItinerary() {
        observationJob?.cancel()
        _uiState.value = ItineraryUiState.Loading
        observationJob = viewModelScope.launch {
            itineraryUpdates
                .map { itinerary ->
                    if (itinerary == null || itinerary.days.isEmpty()) {
                        ItineraryUiState.Empty
                    } else {
                        ItineraryUiState.Success(itinerary)
                    }
                }
                .catch { error ->
                    emit(
                        ItineraryUiState.Error(
                            error.message ?: "Itinerary could not be loaded."
                        )
                    )
                }
                .collect { _uiState.value = it }
        }
    }

    companion object {
        fun factory(tripId: String) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ItineraryViewModel::class.java))
                val repository = ItineraryRepository()
                @Suppress("UNCHECKED_CAST")
                return ItineraryViewModel(
                    itineraryUpdates = repository.observeItinerary(tripId),
                    addActivity = { date, activity ->
                        repository.addActivity(tripId, date, activity)
                    },
                    updateActivity = { target, date, activity ->
                        repository.updateActivity(
                            tripId, target.dayId, target.activity, date, activity
                        )
                    }
                ) as T
            }
        }
    }
}
