package com.humraahi.data

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null
)

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    
    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null) {
            AuthState.Authenticated(auth.currentUser!!.toUser())
        } else {
            AuthState.Unauthenticated
        }
    )
    val authState: Flow<AuthState> = _authState.asStateFlow()
    
    val currentUser get() = auth.currentUser
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        try {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            auth.currentUser?.let {
                _authState.value = AuthState.Authenticated(it.toUser())
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
        }
    }
    
    suspend fun signOut() {
        try {
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign-out failed")
        }
    }
    
    private fun com.google.firebase.auth.FirebaseUser.toUser() = User(
        id = uid,
        name = displayName ?: "",
        email = email ?: "",
        photoUrl = photoUrl?.toString()
    )
}
