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
import com.jvn.myapplication.ui.auth.LoginApprovalScreen
import com.jvn.myapplication.ui.main.AuthState
import com.jvn.myapplication.ui.main.MainAuthViewModel
import com.jvn.myapplication.ui.navigation.MainNavigation
import com.jvn.myapplication.ui.face.LoginFaceVerificationScreen
import com.jvn.myapplication.utils.FirebaseTestHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private var _pendingLoginApproval = mutableStateOf<LoginApprovalData?>(null)
    val pendingLoginApproval: State<LoginApprovalData?> = _pendingLoginApproval
    
    fun clearPendingLoginApproval() {
        _pendingLoginApproval.value = null
    }
    
    data class LoginApprovalData(
        val pendingAuthId: String,
        val username: String,
        val ip: String,
        val location: String
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
        // Test Firebase setup
        FirebaseTestHelper.logFirebaseInfo()
        
        // Test Firebase setup in background
        CoroutineScope(Dispatchers.IO).launch {
            val isSetupSuccessful = FirebaseTestHelper.testFirebaseSetup(this@MainActivity)
            Log.d("Firebase", if (isSetupSuccessful) "🎉 Firebase setup complete!" else "❌ Firebase setup failed!")
        }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")
            
            // Send token to backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val authRepository = AuthRepository(this@MainActivity)
                    authRepository.updateFcmToken(token)
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to update FCM token", e)
                }
            }
        }
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            "LOGIN_APPROVAL" -> {
                val pendingAuthId = intent.getStringExtra("pendingAuthId")
                val username = intent.getStringExtra("username")
                val ip = intent.getStringExtra("ip")
                val location = intent.getStringExtra("location")
                
                if (pendingAuthId != null && username != null && ip != null && location != null) {
                    _pendingLoginApproval.value = LoginApprovalData(pendingAuthId, username, ip, location)
                    Log.d("MainActivity", "🔔 Login approval request received for user: $username")
                }
            }
            "LOGIN_APPROVAL_FACE_VERIFICATION" -> {
                val pendingAuthId = intent.getStringExtra("pendingAuthId")
                if (pendingAuthId != null) {
                    // This will trigger face verification directly
                    _pendingLoginApproval.value = LoginApprovalData(
                        pendingAuthId = pendingAuthId,
                        username = "Quick Approval",
                        ip = "Unknown",
                        location = "Unknown"
                    )
                    Log.d("MainActivity", "🔐 Quick face verification triggered")
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
    
    // Get the activity to access pendingLoginApproval
    val activity = context as? MainActivity
    val pendingApproval by (activity?.pendingLoginApproval ?: remember { mutableStateOf(null) })

    println("DEBUG: Current authState in UI: $authState")
    println("DEBUG: Pending approval: $pendingApproval")

    // Check if we have a pending login approval to handle - show immediately regardless of auth state
    val currentPendingApproval = pendingApproval
    if (currentPendingApproval != null) {
        LoginApprovalScreen(
            pendingAuthId = currentPendingApproval.pendingAuthId,
            username = currentPendingApproval.username,
            ip = currentPendingApproval.ip,
            location = currentPendingApproval.location,
            faceAuthRepository = faceAuthRepository,
            onApprovalComplete = {
                // Clear the pending approval and return to main app
                activity?.clearPendingLoginApproval()
            },
            onDeny = {
                // Clear the pending approval and return to main app
                activity?.clearPendingLoginApproval()
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
            AuthState.REQUIRES_FACE_VERIFICATION -> {
                // Show face verification screen for login
                LoginFaceVerificationScreen(
                    faceAuthRepository = faceAuthRepository,
                    onVerificationSuccess = {
                        mainAuthViewModel.completeFaceVerification()
                    },
                    onLogout = { 
                        // User chooses to logout instead of verifying
                        mainAuthViewModel.logout()
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