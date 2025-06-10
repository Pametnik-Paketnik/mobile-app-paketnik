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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    timestamp: String?,
    onAuthComplete: (Boolean, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var isCapturing by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var authResult by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var captureTriggered by remember { mutableStateOf(false) }
    
    val faceAuth2FAService = remember { FaceAuth2FAService(context) }
    
    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCamera = true
        }
    }
    
    // Format timestamp for display
    val formattedTime = remember {
        timestamp?.let {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val displayFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                val date = isoFormat.parse(it)
                date?.let { displayFormat.format(it) }
            } catch (e: Exception) {
                "Unknown time"
            }
        } ?: "Unknown time"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF008C9E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Face Authentication",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF008C9E)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "🔐 Face Authentication Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF008C9E),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "A login request was made on $formattedTime",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Please verify your identity using face recognition to complete the login process.",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Camera Section or Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (showCamera && hasCameraPermission) {
                // Camera Preview with manual capture
                Box(modifier = Modifier.fillMaxSize()) {
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Position your face in the center and tap the capture button",
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
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
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF008C9E)
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
            } else {
                // Instructions or Permission Request
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!hasCameraPermission) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Camera Permission",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Camera Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please allow camera access to complete face authentication",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF008C9E)
                            )
                        ) {
                            Text("Grant Camera Permission")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face Authentication",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF008C9E)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Ready for Face Authentication",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Position your face clearly in front of the camera and tap the capture button when ready",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { showCamera = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF008C9E)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
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
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deny")
            }
        }
        
        // Processing Indicator
        if (isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF008C9E)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Processing face authentication...",
                        color = Color.DarkGray
                    )
                }
            }
        }
        
        // Show result if available
        authResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.contains("successful", ignoreCase = true)) 
                        Color.Green.copy(alpha = 0.1f) 
                    else 
                        Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    color = if (result.contains("successful", ignoreCase = true)) 
                        Color.Green 
                    else 
                        Color.Red,
                    textAlign = TextAlign.Center
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