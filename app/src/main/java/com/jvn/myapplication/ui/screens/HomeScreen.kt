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
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.ui.main.QRCodeScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories
    val authRepository = remember { AuthRepository(context) }

    // State variables
    var isScanningActive by remember { mutableStateOf(false) }
    
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



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Clean header with solid Airbnb red
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Solid background - no gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(airbnbRed)
            )

            // Centered header content with horizontal layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(600))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Horizontal layout: icon next to text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Air-Box",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (username != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Hello, $username! 👋",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isScanningActive) {
            // QR Scanner UI - Full screen camera view
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
                    // Large camera view for better scanning
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        QRCodeScanner(
                            onQrCodeScanned = { boxId ->
                                isScanningActive = false
                                Toast.makeText(context, "✅ QR Code scanned! Box ID: $boxId", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { isScanningActive = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Scanning", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            // Main dashboard content - true centering between header bottom and bottom nav top
            AnimatedVisibility(
                visible = isContentVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(1000))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Account for the spacer after header (32dp)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Remaining space divided equally above and below button
                    // Total available height minus header(140dp) + spacer(32dp) + bottom nav(100dp) = remaining space
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Centered button - no shadow to avoid visual offset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Main scan button without shadow for precise centering
                        Card(
                            modifier = Modifier.size(180.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = airbnbRed),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No shadow
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                            text = "OPEN\nTHE BOX",
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
                    
                    // Equal space below button
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Account for bottom nav height (100dp)
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
} 