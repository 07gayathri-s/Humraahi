package com.humraahi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(
        ProfileUiState(name = auth.currentUser?.displayName.orEmpty())
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun saveUserName(name: String) {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Sign in again before updating your profile."
            )
            return
        }

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Display name cannot be empty."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                isSaved = false
            )

            try {
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                user.updateProfile(request).await()
                _uiState.value = ProfileUiState(
                    name = trimmedName,
                    isSaved = true
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Profile could not be updated."
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isSaved = false
        )
    }
}

data class ProfileUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)
