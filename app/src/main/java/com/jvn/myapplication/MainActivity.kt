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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private var pendingLoginApproval: LoginApprovalData? = null
    
    data class LoginApprovalData(
        val pendingAuthId: String,
        val username: String,
        val ip: String,
        val location: String
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }
    
    private fun initializeFirebase() {
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
    
    private fun handleNotificationIntent(intent: Intent) {
        when (intent.action) {
            "LOGIN_APPROVAL" -> {
                val pendingAuthId = intent.getStringExtra("pendingAuthId")
                val username = intent.getStringExtra("username")
                val ip = intent.getStringExtra("ip")
                val location = intent.getStringExtra("location")
                
                if (pendingAuthId != null && username != null && ip != null && location != null) {
                    pendingLoginApproval = LoginApprovalData(pendingAuthId, username, ip, location)
                }
            }
            "LOGIN_APPROVAL_FACE_VERIFICATION" -> {
                val pendingAuthId = intent.getStringExtra("pendingAuthId")
                if (pendingAuthId != null) {
                    // This will trigger face verification directly
                    pendingLoginApproval = LoginApprovalData(
                        pendingAuthId = pendingAuthId,
                        username = "Quick Approval",
                        ip = "Unknown",
                        location = "Unknown"
                    )
                }
            }
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
    val pendingApproval = activity?.pendingLoginApproval

    println("DEBUG: Current authState in UI: $authState")
    println("DEBUG: Pending approval: $pendingApproval")

    // Check if we have a pending login approval to handle
    if (pendingApproval != null && authState == AuthState.AUTHENTICATED) {
        LoginApprovalScreen(
            pendingAuthId = pendingApproval.pendingAuthId,
            username = pendingApproval.username,
            ip = pendingApproval.ip,
            location = pendingApproval.location,
            faceAuthRepository = faceAuthRepository,
            onApprovalComplete = {
                // Clear the pending approval and return to main app
                activity.pendingLoginApproval = null
            },
            onDeny = {
                // Clear the pending approval and return to main app
                activity.pendingLoginApproval = null
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