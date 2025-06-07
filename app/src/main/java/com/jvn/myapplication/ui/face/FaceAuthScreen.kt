package com.jvn.myapplication.ui.face

import android.Manifest
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
    val teal = Color(0xFF008C9E)

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

    LaunchedEffect(uiState.isRegistrationComplete) {
        if (uiState.isRegistrationComplete) {
            onRegistrationSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Face Authentication Setup") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = teal,
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
                    teal = teal,
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
                    teal = teal,
                    currentStep = uiState.currentStep,
                    isTrainingComplete = uiState.isTrainingComplete,
                    trainingStatus = uiState.trainingStatus
                )
            }

            recordedVideoUri != null -> {
                ReviewContent(
                    teal = teal,
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
                    teal = teal,
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
    teal: Color,
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
                    tint = teal,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Face Authentication Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = teal,
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
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Text("Start Recording", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RecordingContent(
    teal: Color,
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
                color = teal
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
                containerColor = if (isRecording) Color.Red else teal
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
    teal: Color,
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
                    color = teal
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
                        colors = ButtonDefaults.buttonColors(containerColor = teal)
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
    teal: Color,
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
                color = teal
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
            color = if (isTrainingComplete) Color(0xFF4CAF50) else teal
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