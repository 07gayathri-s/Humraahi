package com.humraahi.ui.home
import com.humraahi.model.Trip

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val trips: List<Trip>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}