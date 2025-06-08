package com.jvn.myapplication.ui.face

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun FaceCamera(
    onPhotoCaptured: (File) -> Unit,
    onError: (String) -> Unit,
    shouldCapture: Boolean,
    onCaptureHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        onDispose {
            cameraExecutor?.shutdown()
        }
    }

    // Handle capture trigger
    LaunchedEffect(shouldCapture) {
        if (shouldCapture && imageCapture != null) {
            val photoFile = File(
                context.cacheDir,
                "face_verification_${System.currentTimeMillis()}.jpg"
            )

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture?.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        onPhotoCaptured(photoFile)
                        onCaptureHandled()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("FaceCamera", "Photo capture failed", exception)
                        onError("Photo capture failed: ${exception.message}")
                        onCaptureHandled()
                    }
                }
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                
                imageCapture = imageCaptureBuilder.build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA, // Use front camera for face verification
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("FaceCamera", "Camera binding failed", e)
                    onError("Camera initialization failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
} 