// File: data/repository/FaceVerificationRepository.kt
package com.jvn.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.*
import com.jvn.myapplication.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.sqrt

class FaceVerificationRepository(private val context: Context) {
    private val faceVerificationApi = NetworkModule.faceVerificationApi
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(faceDetectorOptions)

    suspend fun processVideoAndSubmit(videoUri: Uri, userId: String): Result<String> {
        return try {
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            // Extract frames from video
            val frameData = extractFramesFromVideo(videoUri)
            val captureCompletedAt = System.currentTimeMillis()

            // Process frames with face detection
            val processedFrames = processFramesWithFaceDetection(frameData)

            // Select best frames
            val selectedFrames = selectBestFrames(processedFrames)

            // Create metadata
            val metadata = createMetadata(
                sessionId = sessionId,
                userId = userId,
                timestamp = startTime,
                captureCompletedAt = captureCompletedAt,
                originalFrames = frameData,
                processedFrames = processedFrames,
                selectedFrames = selectedFrames
            )

            // Convert selected frames to base64
            val framesBase64 = selectedFrames.map { frame ->
                bitmapToBase64(frame.bitmap, 80)
            }

            // Submit to backend
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No auth token available"))
            }

            val request = FaceVerificationRequest(
                sessionId = sessionId,
                metadata = metadata,
                framesBase64 = framesBase64
            )

            val response = faceVerificationApi.submitFaceVerification("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val verificationResponse = response.body()!!
                if (verificationResponse.success) {
                    Result.success("Face verification completed successfully")
                } else {
                    Result.failure(Exception(verificationResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Face verification failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            Log.e("FaceVerificationRepo", "Error processing video", e)
            Result.failure(e)
        }
    }

    private fun extractFramesFromVideo(videoUri: Uri): List<FrameData> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<FrameData>()

        try {
            retriever.setDataSource(context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frameRate = 30 // Assume 30fps
            val intervalMs = 200L // Extract frame every 200ms (5 fps)

            var currentTime = 0L
            var frameIndex = 0

            while (currentTime < duration) {
                try {
                    val bitmap = retriever.getFrameAtTime(currentTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        frames.add(FrameData(
                            bitmap = bitmap,
                            timestampMs = currentTime,
                            frameIndex = frameIndex++
                        ))
                    }
                } catch (e: Exception) {
                    Log.w("FaceVerificationRepo", "Failed to extract frame at $currentTime ms", e)
                }

                currentTime += intervalMs
            }

        } finally {
            retriever.release()
        }

        return frames
    }

    private suspend fun processFramesWithFaceDetection(frames: List<FrameData>): List<ProcessedFrame> {
        val processedFrames = mutableListOf<ProcessedFrame>()

        for (frame in frames) {
            try {
                val inputImage = InputImage.fromBitmap(frame.bitmap, 0)
                val faces = detector.process(inputImage).await()

                if (faces.isNotEmpty()) {
                    val face = faces[0] // Use the first detected face
                    val isBlurry = detectBlur(frame.bitmap)

                    processedFrames.add(ProcessedFrame(
                        frameData = frame,
                        face = face,
                        faceConfidence = calculateFaceConfidence(face),
                        isBlurry = isBlurry,
                        lightingQuality = assessLightingQuality(frame.bitmap)
                    ))
                }
            } catch (e: Exception) {
                Log.w("FaceVerificationRepo", "Failed to process frame ${frame.frameIndex}", e)
            }
        }

        return processedFrames
    }

    private fun selectBestFrames(processedFrames: List<ProcessedFrame>): List<ProcessedFrame> {
        // Sort by face confidence and select top frames
        return processedFrames
            .filter { !it.isBlurry && it.faceConfidence > 0.7f }
            .sortedByDescending { it.faceConfidence }
            .take(10) // Select top 10 frames
            .sortedBy { it.frameData.timestampMs } // Sort back by timestamp
    }

    private fun createMetadata(
        sessionId: String,
        userId: String,
        timestamp: Long,
        captureCompletedAt: Long,
        originalFrames: List<FrameData>,
        processedFrames: List<ProcessedFrame>,
        selectedFrames: List<ProcessedFrame>
    ): FaceVerificationMetadata {

        val confidences = processedFrames.map { it.faceConfidence }
        val faceSizes = processedFrames.map { calculateFaceSize(it.face) }

        return FaceVerificationMetadata(
            sessionId = sessionId,
            userId = userId,
            timestamp = timestamp,
            captureCompletedAt = captureCompletedAt,
            totalCaptureTimeMs = captureCompletedAt - timestamp,
            originalFrameCount = originalFrames.size,
            selectedFrameCount = selectedFrames.size,
            frameSelectionCriteria = "confidence_and_quality_based",
            averageFaceConfidence = confidences.average().toFloat(),
            minFaceConfidence = confidences.minOrNull() ?: 0f,
            maxFaceConfidence = confidences.maxOrNull() ?: 0f,
            averageFaceSize = faceSizes.average().toFloat(),
            lightingQuality = assessOverallLightingQuality(processedFrames),
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            cameraResolution = "${originalFrames.firstOrNull()?.bitmap?.width ?: 0}x${originalFrames.firstOrNull()?.bitmap?.height ?: 0}",
            orientationChanges = 0, // Could be tracked during capture
            frameMetrics = selectedFrames.map { createFrameMetric(it) }
        )
    }

    private fun createFrameMetric(processedFrame: ProcessedFrame): FrameMetric {
        val face = processedFrame.face
        val bounds = face.boundingBox

        return FrameMetric(
            frameIndex = processedFrame.frameData.frameIndex,
            timestampMs = processedFrame.frameData.timestampMs,
            faceConfidence = processedFrame.faceConfidence,
            faceBounds = FaceBounds(
                left = bounds.left.toFloat(),
                top = bounds.top.toFloat(),
                right = bounds.right.toFloat(),
                bottom = bounds.bottom.toFloat()
            ),
            headPose = HeadPose(
                pitch = face.headEulerAngleX,
                yaw = face.headEulerAngleY,
                roll = face.headEulerAngleZ
            ),
            eyesOpen = (face.leftEyeOpenProbability ?: 0.5f) > 0.5f && (face.rightEyeOpenProbability ?: 0.5f) > 0.5f,
            isBlurry = processedFrame.isBlurry,
            compressionQuality = 80
        )
    }

    private fun calculateFaceConfidence(face: Face): Float {
        // Combine various confidence metrics
        val baseConfidence = 0.8f // ML Kit doesn't provide direct confidence
        val eyeConfidence = ((face.leftEyeOpenProbability ?: 0.5f) + (face.rightEyeOpenProbability ?: 0.5f)) / 2f
        val smileConfidence = face.smilingProbability ?: 0.5f

        return (baseConfidence + eyeConfidence + smileConfidence) / 3f
    }

    private fun calculateFaceSize(face: Face): Float {
        val bounds = face.boundingBox
        return ((bounds.width() * bounds.height()).toFloat() / (1920 * 1080)) // Normalized to 1080p
    }

    private fun detectBlur(bitmap: Bitmap): Boolean {
        // Simple blur detection using variance of Laplacian
        // This is a simplified implementation
        return false // For now, assume no blur
    }

    private fun assessLightingQuality(bitmap: Bitmap): String {
        // Simple lighting assessment based on average brightness
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val brightness = pixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (r + g + b) / 3.0
        }.average()

        return when {
            brightness < 50 -> "poor"
            brightness < 150 -> "adequate"
            else -> "good"
        }
    }

    private fun assessOverallLightingQuality(processedFrames: List<ProcessedFrame>): String {
        val qualityCounts = processedFrames.groupingBy { it.lightingQuality }.eachCount()
        return qualityCounts.maxByOrNull { it.value }?.key ?: "adequate"
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun getAuthToken() = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
}

// Helper data classes
data class FrameData(
    val bitmap: Bitmap,
    val timestampMs: Long,
    val frameIndex: Int
)

data class ProcessedFrame(
    val frameData: FrameData,
    val face: Face,
    val faceConfidence: Float,
    val isBlurry: Boolean,
    val lightingQuality: String
) {
    val bitmap: Bitmap get() = frameData.bitmap
}