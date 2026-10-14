package com.humraahi.ui.itinerary

sealed class ItinerarySaveState {
    object Idle : ItinerarySaveState()
    object Saving : ItinerarySaveState()
    object Success : ItinerarySaveState()
    data class Error(val message: String) : ItinerarySaveState()
}
