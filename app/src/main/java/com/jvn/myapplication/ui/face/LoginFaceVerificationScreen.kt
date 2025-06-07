package com.jvn.myapplication.ui.face

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.FaceAuthRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginFaceVerificationScreen(
    faceAuthRepository: FaceAuthRepository,
    onVerificationSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    
    // AirBnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    
    // Create ViewModel for this screen
    val faceAuthViewModel: FaceAuthViewModel = viewModel {
        FaceAuthViewModel(faceAuthRepository, "current_user") // userId not needed for verification
    }
    
    val uiState by faceAuthViewModel.uiState.collectAsState()
    
    // Reset state when screen is created
    LaunchedEffect(Unit) {
        faceAuthViewModel.resetState()
    }
    
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            faceAuthViewModel.verifyFace(capturedImageUri!!)
        }
    }

    // Handle verification success
    LaunchedEffect(uiState.isVerificationComplete) {
        if (uiState.isVerificationComplete && uiState.isAuthenticated) {
            delay(1500) // Show success message briefly
            onVerificationSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Face Verification Required") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = airbnbRed,
                titleContentColor = Color.White
            ),
            actions = {
                TextButton(
                    onClick = onLogout
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.isVerifying -> {
                    // Verifying state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = airbnbRed
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Verifying Your Identity",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = airbnbRed
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = uiState.currentStep,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                uiState.isVerificationComplete -> {
                    // Verification result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isAuthenticated) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (uiState.isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = if (uiState.isAuthenticated) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = if (uiState.isAuthenticated) "Identity Verified!" else "Verification Failed",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isAuthenticated) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (uiState.isAuthenticated) 
                                    "Welcome back! Identity verified with ${(uiState.verificationProbability * 100).toInt()}% confidence."
                                else 
                                    "Identity could not be verified (${(uiState.verificationProbability * 100).toInt()}% confidence). Please try again or logout to sign in with a different account.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )

                            if (!uiState.isAuthenticated) {
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                                                    Button(
                                        onClick = {
                                            // Reset state and try again
                                            faceAuthViewModel.clearMessages()
                                            faceAuthViewModel.resetState()
                                            capturedImageUri = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                                    ) {
                                        Text("Try Again")
                                    }
                            }
                        }
                    }
                }

                else -> {
                    // Initial state - show scan button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = airbnbRed
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Face Verification Required",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = airbnbRed,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "You have Face Verification (2FA) enabled. Please scan your face to continue.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                        == PackageManager.PERMISSION_GRANTED) {
                                        // Create image URI and launch camera
                                        val uri = context.contentResolver.insert(
                                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            ContentValues().apply {
                                                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "face_login_${System.currentTimeMillis()}.jpg")
                                                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                            }
                                        )
                                        capturedImageUri = uri
                                        uri?.let { cameraLauncher.launch(it) }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Face to Continue")
                            }


                        }
                    }
                }
            }

            // Error handling
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    delay(3000)
                    faceAuthViewModel.clearMessages()
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
} 