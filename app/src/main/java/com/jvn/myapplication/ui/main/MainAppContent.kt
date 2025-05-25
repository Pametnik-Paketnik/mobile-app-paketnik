// File: ui/main/MainAppContent.kt
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
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val teal = Color(0xFF008C9E)
    val lightGray = Color(0xFFF5F5F5)

    val context = LocalContext.current

    var isScanningActive by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningActive = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
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
            )
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
                        // Display boxId as toast when QR code is scanned
                        Toast.makeText(context, "Box ID: $boxId", Toast.LENGTH_LONG).show()

                        // After scanning, return to the main screen
                        isScanningActive = false
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