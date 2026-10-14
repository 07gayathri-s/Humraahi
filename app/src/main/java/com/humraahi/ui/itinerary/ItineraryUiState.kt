package com.humraahi.ui.itinerary

import com.humraahi.model.Itinerary

sealed class ItineraryUiState {
    object Loading : ItineraryUiState()
    object Empty : ItineraryUiState()
    data class Success(val itinerary: Itinerary) : ItineraryUiState()
    data class Error(val message: String) : ItineraryUiState()
}
