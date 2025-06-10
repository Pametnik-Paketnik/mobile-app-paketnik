package com.jvn.myapplication.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jvn.myapplication.ui.main.QRCodeScanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenBoxScreen(
    reservationId: Int,
    onBackClick: () -> Unit,
    onSuccess: (String) -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00C851)

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(airbnbRed)
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open the Box",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSuccess) {
                // Success state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = successGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Success!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Text(
                            text = "Box operation completed successfully",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Done",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Instructions card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                                                 Icon(
                             imageVector = Icons.Default.Lock,
                             contentDescription = null,
                             tint = airbnbRed,
                             modifier = Modifier.size(48.dp)
                         )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scan QR Code",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Text(
                            text = "Point your camera at the QR code on the box to open it",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code Scanner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (isProcessing) {
                            // Processing state
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = airbnbRed)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Processing...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textLight
                                )
                            }
                        } else {
                            // QR Scanner
                            QRCodeScanner(
                                onQrCodeScanned = { code ->
                                    if (!isProcessing) {
                                        scannedCode = code
                                        isProcessing = true
                                        
                                        // Simulate processing delay
                                        kotlinx.coroutines.MainScope().launch {
                                            kotlinx.coroutines.delay(2000)
                                            showSuccess = true
                                            onSuccess(code)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color.Black,
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info text
                Text(
                    text = "Reservation #$reservationId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 