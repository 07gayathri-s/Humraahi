package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humraahi.data.ItineraryRepository
import com.humraahi.model.Itinerary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ItineraryViewModel(
    private val itineraryUpdates: Flow<Itinerary?>
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Loading)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeItinerary()
    }

    fun retry() {
        observeItinerary()
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
                return ItineraryViewModel(repository.observeItinerary(tripId)) as T
            }
        }
    }
}
