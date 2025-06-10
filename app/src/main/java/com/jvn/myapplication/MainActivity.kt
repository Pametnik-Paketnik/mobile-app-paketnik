// File: MainActivity.kt (Updated with Tab Navigation)
package com.jvn.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.messaging.FirebaseMessaging
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import com.jvn.myapplication.ui.auth.AuthScreen
import com.jvn.myapplication.ui.auth.AuthViewModel
import com.jvn.myapplication.ui.screens.FaceAuth2FAScreen
import com.jvn.myapplication.ui.main.AuthState
import com.jvn.myapplication.ui.main.MainAuthViewModel
import com.jvn.myapplication.ui.navigation.MainNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private var _pendingFaceAuth2FA = mutableStateOf<FaceAuth2FAData?>(null)
    val pendingFaceAuth2FA: State<FaceAuth2FAData?> = _pendingFaceAuth2FA
    
    fun clearPendingFaceAuth2FA() {
        _pendingFaceAuth2FA.value = null
    }
    
    data class FaceAuth2FAData(
        val requestId: String,
        val timestamp: String?
    )
    
    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            Log.d("MainActivity", "❌ Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Initialize Firebase and get FCM token
        initializeFirebase()
        
        // Handle intent from notification
        handleNotificationIntent(intent)
        
        setContent {
            MaterialTheme {
                Direct4meApp()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun initializeFirebase() {
        Log.d("FCM_2FA", "🔥 Initializing Firebase for 2FA...")
        
        // Get FCM token and register device with backend for 2FA notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_2FA", "❌ Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM_2FA", "📱 FCM Token: ${token.take(20)}...")
            
            // Register device with backend for 2FA notifications
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val authRepository = AuthRepository(this@MainActivity)
                    
                    // Check if user is authenticated before registering device
                    val authToken = authRepository.getAuthToken().first()
                    if (!authToken.isNullOrEmpty()) {
                        // User is authenticated, register device
                        Log.d("FCM_2FA", "🔐 User authenticated, registering device...")
                        
                        val result = authRepository.registerDevice(token)
                        result.onSuccess { deviceResponse ->
                            Log.d("FCM_2FA", "✅ Device registration successful: ${deviceResponse.message}")
                        }.onFailure { exception ->
                            Log.e("FCM_2FA", "❌ Device registration failed: ${exception.message}")
                            // Fallback to old FCM token update if device registration fails
                            authRepository.updateFcmToken(token)
                        }
                    } else {
                        // User not authenticated, just store token for later registration
                        Log.d("FCM_2FA", "📝 User not authenticated, storing token for later registration")
                        // We could store the token locally and register it after login
                    }
                } catch (e: Exception) {
                    Log.e("FCM_2FA", "💥 Error during device registration: ${e.message}")
                }
            }
        }
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            "FACE_AUTH_REQUEST" -> {
                val requestId = intent.getStringExtra("requestId")
                val timestamp = intent.getStringExtra("timestamp")
                
                if (requestId != null) {
                    _pendingFaceAuth2FA.value = FaceAuth2FAData(requestId, timestamp)
                    Log.d("MainActivity", "🔐 2FA Face auth request received: $requestId")
                }
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Notification permission already granted")
                }
                else -> {
                    Log.d("MainActivity", "🔔 Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("MainActivity", "📱 Notification permission not required (Android < 13)")
        }
    }
}

@Composable
fun Direct4meApp() {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val faceAuthRepository = remember { FaceAuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
    val mainAuthViewModel: MainAuthViewModel = viewModel { MainAuthViewModel(authRepository, faceAuthRepository) }

    val authState by mainAuthViewModel.authState.collectAsState()
    
    // Get the activity to access pending 2FA face auth
    val activity = context as? MainActivity
    val pendingFaceAuth by (activity?.pendingFaceAuth2FA ?: remember { mutableStateOf(null) })

    println("DEBUG: Current authState in UI: $authState")
    println("DEBUG: Pending 2FA Face Auth: $pendingFaceAuth")

    // Check if we have a pending 2FA face auth request - show immediately regardless of auth state
    val currentPendingFaceAuth = pendingFaceAuth
    if (currentPendingFaceAuth != null) {
        FaceAuth2FAScreen(
            requestId = currentPendingFaceAuth.requestId,
            timestamp = currentPendingFaceAuth.timestamp,
            onAuthComplete = { success, message ->
                Log.d("2FA", if (success) "✅ Face auth successful: $message" else "❌ Face auth failed: $message")
                // Clear the pending 2FA request and return to main app
                activity?.clearPendingFaceAuth2FA()
            },
            onCancel = {
                Log.d("2FA", "❌ User cancelled face auth")
                // Clear the pending 2FA request and return to main app
                activity?.clearPendingFaceAuth2FA()
            }
        )
    } else {
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
                // Show main app with tab navigation
                MainNavigation(
                    onLogout = {
                        mainAuthViewModel.logout()
                    }
                )
            }
        }
    }
}