// File: ui/face/FaceRecordingCamera.kt
package com.jvn.myapplication.ui.face

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun FaceRecordingCamera(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    onVideoRecorded: (Uri) -> Unit,
    onRecordingStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        onDispose {
            cameraExecutor?.shutdown()
        }
    }

    // Handle recording state changes
    LaunchedEffect(isRecording) {
        if (isRecording && recording == null) {
            // Start recording
            videoCapture?.let { capture ->
                val outputFile = createVideoFile(context)
                val outputOptions = FileOutputOptions.Builder(outputFile).build()

                recording = capture.output
                    .prepareRecording(context, outputOptions)
                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                        when (recordEvent) {
                            is VideoRecordEvent.Start -> {
                                Log.d("FaceRecording", "Recording started")
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (!recordEvent.hasError()) {
                                    Log.d("FaceRecording", "Recording saved: ${outputFile.absolutePath}")
                                    onVideoRecorded(Uri.fromFile(outputFile))
                                } else {
                                    Log.e("FaceRecording", "Recording error: ${recordEvent.error}")
                                }
                                recording = null
                            }
                        }
                    }

                // Auto-stop recording after 10 seconds
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    delay(10000)
                    recording?.stop()
                    onRecordingStateChanged(false)
                }
            }
        } else if (!isRecording && recording != null) {
            // Stop recording
            recording?.stop()
            recording = null
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        videoCapture
                    )
                } catch (e: Exception) {
                    Log.e("FaceRecording", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun createVideoFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "face_verification_$timestamp.mp4"
    return File(context.cacheDir, fileName)
}