package com.jvn.myapplication.ui.face

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceAuthScreen(
    faceAuthViewModel: FaceAuthViewModel,
    onRegistrationSuccess: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val uiState by faceAuthViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // AirBnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    var showInstructions by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var recordingProgress by remember { mutableStateOf(0f) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showInstructions = false
        }
    }

    // Auto-progress recording indicator
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingProgress = 0f
            repeat(100) { i ->
                delay(100) // 10 seconds = 10000ms, 100 steps = 100ms each
                recordingProgress = (i + 1) / 100f
            }
        } else {
            recordingProgress = 0f
        }
    }

    // Don't automatically go back - let user complete the full flow

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Face Authentication Setup") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = airbnbRed,
                titleContentColor = Color.White
            ),
            actions = {
                TextButton(
                    onClick = onSkip
                ) {
                    Text("Skip", color = Color.White)
                }
            }
        )

        when {
            showInstructions -> {
                InstructionsContent(
                    airbnbRed = airbnbRed,
                    onStartRecording = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            showInstructions = false
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }

            uiState.isProcessing -> {
                ProcessingContent(
                    airbnbRed = airbnbRed,
                    currentStep = uiState.currentStep,
                    isTrainingComplete = uiState.isTrainingComplete,
                    trainingStatus = uiState.trainingStatus
                )
            }

            uiState.isRegistrationComplete && !uiState.isTrainingComplete -> {
                TrainingStatusContent(
                    airbnbRed = airbnbRed,
                    faceAuthViewModel = faceAuthViewModel,
                    trainingStatus = uiState.trainingStatus,
                    statusMessage = uiState.statusMessage
                )
            }

            uiState.isTrainingComplete && !uiState.isVerificationComplete -> {
                FaceVerificationTestContent(
                    airbnbRed = airbnbRed,
                    faceAuthViewModel = faceAuthViewModel,
                    isVerifying = uiState.isVerifying,
                    currentStep = uiState.currentStep
                )
            }

            uiState.isVerificationComplete -> {
                VerificationResultContent(
                    airbnbRed = airbnbRed,
                    isAuthenticated = uiState.isAuthenticated,
                    probability = uiState.verificationProbability,
                    onComplete = onRegistrationSuccess
                )
            }

            recordedVideoUri != null -> {
                ReviewContent(
                    airbnbRed = airbnbRed,
                    onSubmit = {
                        recordedVideoUri?.let { uri ->
                            faceAuthViewModel.registerWithVideo(uri)
                        }
                    },
                    onRetake = {
                        recordedVideoUri = null
                    }
                )
            }

            else -> {
                RecordingContent(
                    airbnbRed = airbnbRed,
                    isRecording = isRecording,
                    recordingProgress = recordingProgress,
                    onVideoRecorded = { uri ->
                        recordedVideoUri = uri
                        isRecording = false
                    },
                    onRecordingStateChanged = { recording ->
                        isRecording = recording
                    }
                )
            }
        }

        // Error handling
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Show error for 3 seconds
                delay(3000)
                faceAuthViewModel.clearMessages()
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

@Composable
private fun InstructionsContent(
    airbnbRed: Color,
    onStartRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                    tint = airbnbRed,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Face Authentication Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = airbnbRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Record a 10-second video to train your face model. This will extract ~60 frames for optimal authentication.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "• Look directly at the camera",
                    "• Slowly turn your head left and right",
                    "• Ensure good lighting",
                    "• Keep your face in the frame for 10 seconds"
                ).forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                ) {
                    Text("Start Recording", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RecordingContent(
    airbnbRed: Color,
    isRecording: Boolean,
    recordingProgress: Float,
    onVideoRecorded: (Uri) -> Unit,
    onRecordingStateChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isRecording) "Recording... ${(recordingProgress * 10).toInt()}/10 seconds" 
                  else "Position your face in the frame",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Recording progress
        if (isRecording) {
            LinearProgressIndicator(
                progress = recordingProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                color = airbnbRed
            )
        }

        // Camera preview area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            FaceRecordingCamera(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                isRecording = isRecording,
                onVideoRecorded = onVideoRecorded,
                onRecordingStateChanged = onRecordingStateChanged
            )
        }

        // Record button
        Button(
            onClick = { onRecordingStateChanged(true) },
            modifier = Modifier
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else airbnbRed
            ),
            enabled = !isRecording
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Warning else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Stop" else "Record",
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReviewContent(
    airbnbRed: Color,
    onSubmit: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recording Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = airbnbRed
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your 10-second video has been recorded. We'll extract ~60 frames to train your face model.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retake")
                    }

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                    ) {
                        Text("Process Video")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent(
    airbnbRed: Color,
    currentStep: String,
    isTrainingComplete: Boolean,
    trainingStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isTrainingComplete) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = airbnbRed
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isTrainingComplete) "Setup Complete!" else "Setting Up Face Authentication...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isTrainingComplete) Color(0xFF4CAF50) else airbnbRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = currentStep,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        if (trainingStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: $trainingStatus",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrainingStatusContent(
    airbnbRed: Color,
    faceAuthViewModel: FaceAuthViewModel,
    trainingStatus: String,
    statusMessage: String
) {
    // Auto-check training status every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            faceAuthViewModel.checkTrainingStatus()
            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                    text = "Training Your Face Model",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = airbnbRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Please wait while we process your video frames and train the face recognition model. This may take a few minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Status: $trainingStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceVerificationTestContent(
    airbnbRed: Color,
    faceAuthViewModel: FaceAuthViewModel,
    isVerifying: Boolean,
    currentStep: String
) {
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            faceAuthViewModel.verifyFace(capturedImageUri!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = airbnbRed
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Verifying Your Face",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = airbnbRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentStep,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Training Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your face model is ready. Let's test it with a verification scan.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val uri = context.contentResolver.insert(
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                ContentValues().apply {
                                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "face_verify_${System.currentTimeMillis()}.jpg")
                                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                }
                            )
                            capturedImageUri = uri
                            uri?.let { cameraLauncher.launch(it) }
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
                        Text("Scan Face to Verify")
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationResultContent(
    airbnbRed: Color,
    isAuthenticated: Boolean,
    probability: Float,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAuthenticated) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (isAuthenticated) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isAuthenticated) "Face Verification Successful!" else "Face Verification Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAuthenticated) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAuthenticated) 
                        "Identity verified with ${(probability * 100).toInt()}% confidence. Face authentication is now enabled for your account."
                    else 
                        "Identity could not be verified (${(probability * 100).toInt()}% confidence). You may need to re-register your face.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                if (isAuthenticated) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "From now on, you'll need to verify your face when logging in if 2FA is enabled.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAuthenticated) Color(0xFF4CAF50) else airbnbRed
                    )
                ) {
                    Text(if (isAuthenticated) "Complete Setup" else "Try Again")
                }
            }
        }
    }
} 