// File: ui/face/FaceVerificationScreen.kt
package com.jvn.myapplication.ui.face

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerificationScreen(
    faceVerificationViewModel: FaceVerificationViewModel,
    onVerificationSuccess: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val uiState by faceVerificationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val teal = Color(0xFF008C9E)

    var showInstructions by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showInstructions = false
        }
    }

    LaunchedEffect(uiState.isVerificationComplete) {
        if (uiState.isVerificationComplete) {
            onVerificationSuccess()
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
            title = { Text("Face Verification") },
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
                ProcessingContent(teal = teal)
            }

            recordedVideoUri != null -> {
                ReviewContent(
                    teal = teal,
                    onSubmit = {
                        recordedVideoUri?.let { uri ->
                            faceVerificationViewModel.processVideo(uri)
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

        // Error display
        uiState.errorMessage?.let { error: String ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = teal
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Face Verification Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = teal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "For security purposes, please record a 10-second video of your face.",
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
                    "• Keep your face in the frame"
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
            text = if (isRecording) "Recording... Move your head slowly" else "Position your face in the frame",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                isRecording = isRecording,
                onVideoRecorded = onVideoRecorded,
                onRecordingStateChanged = onRecordingStateChanged
            )
        }

        // Recording controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isRecording) {
                Button(
                    onClick = { onRecordingStateChanged(true) },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Recording",
                        tint = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
                    text = "Your video has been recorded successfully. Submit for verification or retake if needed.",
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
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent(teal: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = teal
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Processing Video...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = teal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Analyzing frames and verifying your identity",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}