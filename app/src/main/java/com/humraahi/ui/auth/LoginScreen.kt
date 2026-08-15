package com.humraahi.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.humraahi.data.AuthState
import com.humraahi.navigation.Routes

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.humraahi.R.string.google_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }
    
    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
            else -> Unit
        }
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                // Handle sign-in failure
                e.printStackTrace()
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Humraahi ✈️",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "हमराही · The one who walks the road with you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Group travel planning made simple",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Error -> {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    GoogleSignInButton(
                        onClick = {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    GoogleSignInButton(
                        onClick = {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text("Sign in with Google")
    }
}
