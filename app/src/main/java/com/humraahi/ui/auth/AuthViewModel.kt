package com.humraahi.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.humraahi.data.AuthRepository
import com.humraahi.data.AuthState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    val authState: StateFlow<AuthState> = repository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
    
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            repository.signInWithGoogle(account)
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
