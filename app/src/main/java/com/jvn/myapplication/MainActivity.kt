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
import com.jvn.myapplication.ui.main.MainAppContent
import kotlinx.coroutines.flow.first

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

    // Check if user is already logged in
    var isCheckingAuth by remember { mutableStateOf(true) }
    var isAuthenticated by remember { mutableStateOf(false) }

    // Listen to auth token changes in real-time
    val authToken by authRepository.getAuthToken().collectAsState(initial = null)

    // React to token changes
    LaunchedEffect(authToken) {
        // Update auth state whenever token changes
        isAuthenticated = !authToken.isNullOrEmpty()
        if (isCheckingAuth) {
            isCheckingAuth = false
        }
    }

    when {
        isCheckingAuth -> {
            // Show loading screen while checking authentication
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF008C9E))
            }
        }
        !isAuthenticated -> {
            // Show auth screen if no valid token
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    // Token will be updated automatically, triggering LaunchedEffect
                    // But we can also force immediate update for responsiveness
                    isAuthenticated = true
                }
            )
        }
        else -> {
            // Show main app if authenticated
            MainAppContent()
        }
    }
}