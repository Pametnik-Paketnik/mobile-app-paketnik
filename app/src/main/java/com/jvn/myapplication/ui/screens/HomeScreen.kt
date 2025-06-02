package com.jvn.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceVerificationRepository
import com.jvn.myapplication.ui.face.FaceVerificationScreen
import com.jvn.myapplication.ui.face.FaceVerificationViewModel
import com.jvn.myapplication.ui.main.QRCodeScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Modern color palette
    val primaryTeal = Color(0xFF008C9E)
    val lightTeal = Color(0xFF4DB6AC)
    val darkTeal = Color(0xFF00695C)
    val accentBlue = Color(0xFF2196F3)
    val softGray = Color(0xFFF8F9FA)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF2E2E2E)
    val textLight = Color(0xFF757575)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories
    val authRepository = remember { AuthRepository(context) }
    val faceVerificationRepository = remember { FaceVerificationRepository(context) }

    // State variables
    var isScanningActive by remember { mutableStateOf(false) }
    var showFaceVerification by remember { mutableStateOf(false) }
    
    // User data from repository
    val userId by authRepository.getUserId().collectAsState(initial = null)
    val username by authRepository.getUsername().collectAsState(initial = null)
    val userType by authRepository.getUserType().collectAsState(initial = null)

    // Animation state
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(userId, username, userType) {
        delay(300)
        isContentVisible = true
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningActive = true
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    // Handle face verification screen
    if (showFaceVerification && userId != null) {
        val faceVerificationViewModel: FaceVerificationViewModel = viewModel {
            FaceVerificationViewModel(faceVerificationRepository, userId!!)
        }
        FaceVerificationScreen(
            faceVerificationViewModel = faceVerificationViewModel,
            onVerificationSuccess = {
                showFaceVerification = false
                Toast.makeText(context, "✅ Face verified! Opening box...", Toast.LENGTH_LONG).show()
            },
            onSkip = {
                showFaceVerification = false
                Toast.makeText(context, "⏭️ Face verification skipped", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(softGray)
    ) {
        // Beautiful gradient header
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryTeal, lightTeal, accentBlue),
                            startY = 0f,
                            endY = 600f
                        )
                    )
            )

            // Header content
            Column {
                // Top section with app name and greeting
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Paketnik",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (username != null) {
                                Text(
                                    "Hello, $username! 👋",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Welcome section card
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(800)
                    ) + fadeIn(animationSpec = tween(800))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardWhite),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                primaryTeal.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (userType == "HOST") "🏢" else "👤",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = if (userType == "HOST") "Host Dashboard" else "Smart Access",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = textDark
                                        )
                                        Text(
                                            text = if (userType == "HOST")
                                                "Manage boxes & monitor access"
                                            else
                                                "Secure box access system",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isScanningActive) {
            // QR Scanner UI
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = primaryTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Scan QR Code",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                "Position QR code within the frame",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                    ) {
                        QRCodeScanner(
                            onQrCodeScanned = { boxId ->
                                isScanningActive = false
                                showFaceVerification = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { isScanningActive = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryTeal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Scanning", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            // Main dashboard content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main QR scan button with beautiful animation
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(1000))
                ) {
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow effect
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryTeal.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        radius = 300f
                                    ),
                                    CircleShape
                                )
                        )

                        // Main scan button
                        Card(
                            modifier = Modifier
                                .size(180.dp)
                                .shadow(12.dp, CircleShape),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = cardWhite)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(primaryTeal, darkTeal),
                                            radius = 400f
                                        )
                                    )
                                    .clip(CircleShape),
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
                                    modifier = Modifier.size(160.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Scan",
                                            modifier = Modifier.size(56.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "SCAN\nQR CODE",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Quick info section
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardWhite),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            accentBlue.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = accentBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "How to Use",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textDark
                                    )
                                    Text(
                                        "Tap the button to scan QR codes for secure box access",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textLight
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Extra space for bottom navigation
            }
        }
    }
} 