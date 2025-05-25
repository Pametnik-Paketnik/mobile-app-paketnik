// File: MainActivity.kt
package com.jvn.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.ui.auth.AuthScreen
import com.jvn.myapplication.ui.auth.AuthViewModel
import com.jvn.myapplication.ui.main.AuthState
import com.jvn.myapplication.ui.main.MainAppContent
import com.jvn.myapplication.ui.main.MainAuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Direct4meApp()
            }
        }
    }
}

@Composable
fun Direct4meApp() {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
    val mainAuthViewModel: MainAuthViewModel = viewModel { MainAuthViewModel(authRepository) }

    val authState by mainAuthViewModel.authState.collectAsState()

    println("DEBUG: Current authState in UI: $authState")

    when (authState) {
        AuthState.CHECKING -> {
            // Show loading screen while checking authentication
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF008C9E))
            }
        }
        AuthState.UNAUTHENTICATED -> {
            // Show auth screen if no valid token
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    // This will be handled automatically by token changes
                }
            )
        }
        AuthState.AUTHENTICATED -> {
            // Show main app if authenticated
            MainAppContent(
                onLogout = {
                    mainAuthViewModel.logout()
                }
            )
        }
    }
}