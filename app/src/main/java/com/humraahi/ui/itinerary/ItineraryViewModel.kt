package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humraahi.data.ItineraryRepository
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
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
    private val addActivity: suspend (Int, ItineraryActivity) -> Unit
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
        dayNumber: String,
        title: String,
        startTime: String,
        location: String,
        notes: String
    ) {
        if (_saveState.value is ItinerarySaveState.Saving) return

        val parsedDay = dayNumber.trim().toIntOrNull()
        if (parsedDay == null || parsedDay < 1) {
            _saveState.value = ItinerarySaveState.Error("Day number must be at least 1.")
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

        val activity = ItineraryActivity(
            title = trimmedTitle,
            startTime = time,
            location = location.trim().takeIf { it.isNotEmpty() },
            notes = notes.trim().takeIf { it.isNotEmpty() }
        )
        _saveState.value = ItinerarySaveState.Saving
        viewModelScope.launch {
            try {
                addActivity(parsedDay, activity)
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
                    addActivity = { dayNumber, activity ->
                        repository.addActivity(tripId, dayNumber, activity)
                    }
                ) as T
            }
        }
    }
}
