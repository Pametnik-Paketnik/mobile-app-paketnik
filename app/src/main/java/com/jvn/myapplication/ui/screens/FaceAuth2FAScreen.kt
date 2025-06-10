package com.jvn.myapplication.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jvn.myapplication.services.FaceAuth2FAService
import com.jvn.myapplication.services.FaceAuthResult
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Screen for handling 2FA Face Authentication requests
 * 
 * This screen is shown when the user receives an FCM notification
 * for face authentication and needs to verify their identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceAuth2FAScreen(
    requestId: String,
    timestamp: String? = null,
    onAuthComplete: (Boolean, String) -> Unit,
    onCancel: () -> Unit
) {
    // Airbnb-style color palette (matching the rest of the app)
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00A699)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceAuth2FAService = remember { FaceAuth2FAService(context) }
    
    // Permission handling
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // UI state
    var showCamera by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var authResult by remember { mutableStateOf<String?>(null) }
    var captureTriggered by remember { mutableStateOf(false) }

    // Check camera permission on screen load
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray) // Airbnb light gray background
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Face icon with Airbnb styling
                Card(
                    modifier = Modifier.size(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = airbnbRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face Authentication",
                            modifier = Modifier.size(40.dp),
                            tint = airbnbRed
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Face Authentication Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Please verify your identity to complete the login request",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // Request ID info
                timestamp?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = lightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Request ID: ${requestId.take(20)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = textLight,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Camera/Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> {
                        // Processing state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = airbnbRed,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Processing face authentication...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    authResult != null -> {
                        // Result state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Card(
                                modifier = Modifier.size(80.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = successGreen.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        modifier = Modifier.size(40.dp),
                                        tint = successGreen
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = authResult!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    showCamera && hasCameraPermission -> {
                        // Camera preview
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FaceCameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                captureTriggered = captureTriggered,
                                onImageCaptured = { imageFile ->
                                    captureTriggered = false // Reset trigger
                                    isCapturing = true
                                    isProcessing = true
                                    
                                    lifecycleOwner.lifecycleScope.launch {
                                        val result = faceAuth2FAService.completeFaceAuth(requestId, imageFile)
                                        
                                        isProcessing = false
                                        isCapturing = false
                                        
                                        when (result) {
                                            is FaceAuthResult.Success -> {
                                                authResult = result.message
                                                onAuthComplete(true, result.message)
                                            }
                                            is FaceAuthResult.Error -> {
                                                authResult = result.message
                                                onAuthComplete(false, result.message)
                                            }
                                        }
                                    }
                                },
                                onCaptureComplete = {
                                    captureTriggered = false // Reset trigger after capture attempt
                                }
                            )
                            
                            // Camera overlay instructions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Position your face in the center and tap the capture button",
                                        modifier = Modifier.padding(16.dp),
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // Manual capture button at bottom
                            if (!isCapturing && !isProcessing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(32.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Button(
                                        onClick = {
                                            // Trigger manual capture
                                            captureTriggered = true
                                        },
                                        modifier = Modifier.size(80.dp),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = airbnbRed,
                                            contentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Capture Face",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {
                        // Instructions or Permission Request
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!hasCameraPermission) {
                                Card(
                                    modifier = Modifier.size(80.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = textLight.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Camera Permission",
                                            modifier = Modifier.size(40.dp),
                                            tint = textLight
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Camera Permission Required",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Please allow camera access to complete face authentication",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = airbnbRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Grant Camera Permission",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Card(
                                    modifier = Modifier.size(80.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = airbnbRed.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Face Authentication",
                                            modifier = Modifier.size(40.dp),
                                            tint = airbnbRed
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Ready for Face Authentication",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Position your face clearly in front of the camera and tap the capture button when ready",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = { showCamera = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = airbnbRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Start Camera",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Face Verification")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cancel Button
            OutlinedButton(
                onClick = {
                    lifecycleOwner.lifecycleScope.launch {
                        faceAuth2FAService.denyFaceAuth(requestId)
                        onCancel()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = airbnbRed
                ),
                border = BorderStroke(1.dp, airbnbRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cancel",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FaceCameraPreview(
    modifier: Modifier = Modifier,
    captureTriggered: Boolean,
    onImageCaptured: (File) -> Unit,
    onCaptureComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var shouldCapture by remember { mutableStateOf(false) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    Log.d("FaceAuth2FA", "✅ Camera bound successfully")
                } catch (e: Exception) {
                    Log.e("FaceAuth2FA", "❌ Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
    
    // Manual capture trigger - captures only when triggered
    LaunchedEffect(captureTriggered) {
        if (captureTriggered) {
            // Wait a moment for camera to be ready
            kotlinx.coroutines.delay(500) 
            
            imageCapture?.let { capture ->
                Log.d("FaceAuth2FA", "📸 Taking picture...")
                
                val outputFile = File(
                    context.externalCacheDir,
                    "face_auth_${System.currentTimeMillis()}.jpg"
                )
                
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                
                capture.takePicture(
                    outputFileOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("FaceAuth2FA", "✅ Photo saved: ${outputFile.absolutePath}")
                            onImageCaptured(outputFile)
                            onCaptureComplete()
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("FaceAuth2FA", "❌ Photo capture failed", exception)
                            onCaptureComplete()
                        }
                    }
                )
            } ?: run {
                Log.e("FaceAuth2FA", "❌ ImageCapture not initialized")
                onCaptureComplete()
            }
        }
    }
} 