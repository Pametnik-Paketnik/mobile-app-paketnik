// File: ui/main/MainAppContent.kt (Updated with Face Verification)
package com.jvn.myapplication.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceVerificationRepository
import com.jvn.myapplication.ui.face.FaceVerificationScreen
import com.jvn.myapplication.ui.face.FaceVerificationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    onLogout: () -> Unit = {}
) {
    val teal = Color(0xFF008C9E)
    val lightGray = Color(0xFFF5F5F5)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories and ViewModels
    val authRepository = remember { AuthRepository(context) }
    val faceVerificationRepository = remember { FaceVerificationRepository(context) }

    var isScanningActive by remember { mutableStateOf(false) }
    var showFaceVerification by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }

    // Get user ID when component loads
    LaunchedEffect(Unit) {
        userId = authRepository.getUserId().first()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningActive = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    // Show Face Verification Screen if needed
    if (showFaceVerification && userId != null) {
        val faceVerificationViewModel: FaceVerificationViewModel = viewModel {
            FaceVerificationViewModel(faceVerificationRepository, userId!!)
        }

        FaceVerificationScreen(
            faceVerificationViewModel = faceVerificationViewModel,
            onVerificationSuccess = {
                showFaceVerification = false
                // Here you would proceed with opening the box
                Toast.makeText(context, "Face verification successful! Opening box...", Toast.LENGTH_LONG).show()
            },
            onSkip = {
                showFaceVerification = false
                Toast.makeText(context, "Face verification skipped", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Direct4Me Box Opener") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = teal,
                titleContentColor = Color.White
            ),
            actions = {
                var showLogoutDialog by remember { mutableStateOf(false) }

                TextButton(
                    onClick = { showLogoutDialog = true }
                ) {
                    Text("Logout", color = Color.White)
                }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Logout") },
                        text = { Text("Are you sure you want to logout?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    println("DEBUG: Logout button clicked in MainAppContent")
                                    onLogout()
                                    showLogoutDialog = false
                                }
                            ) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        )

        if (isScanningActive) {
            // Add a spacer to push content down
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Scanning QR code...",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Center the scanner in the remaining space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                QRCodeScanner(
                    onQrCodeScanned = { boxId ->
                        // After QR code is scanned, start face verification
                        isScanningActive = false
                        showFaceVerification = true
                    },
                    modifier = Modifier
                        .size(300.dp)  // Fixed size for the scanner
                        .clip(MaterialTheme.shapes.medium)  // Add rounded corners
                )
            }

            // Put the cancel button at the bottom with some padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { isScanningActive = false },
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Text("Cancel Scanning")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            isScanningActive = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Text(
                        text = "OPEN",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}