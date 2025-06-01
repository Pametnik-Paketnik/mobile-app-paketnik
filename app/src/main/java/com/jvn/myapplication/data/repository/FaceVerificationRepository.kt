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
            Log.d("FaceVerification", "Processing video: $videoUri")

            // Check if file exists
            val fileDescriptor = context.contentResolver.openFileDescriptor(videoUri, "r")
            if (fileDescriptor == null) {
                Log.e("FaceVerification", "Cannot open video file")
                return Result.failure(Exception("Cannot open video file"))
            }
            fileDescriptor.close()

            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            // Extract frames from video
            val frameData = extractFramesFromVideo(videoUri)
            val captureCompletedAt = System.currentTimeMillis()

            // Process frames with face detection
            val processedFrames = processFramesWithFaceDetection(frameData)

            // Select best frames
            val selectedFrames = selectBestFrames(processedFrames)

            if (selectedFrames.isEmpty()) {
                return Result.failure(Exception("No suitable frames found for verification"))
            }

            // Split frames into batches of 2 (reduced from 3)
            val batches = selectedFrames.chunked(2)
            Log.d("FaceVerification", "Sending ${batches.size} batches with total ${selectedFrames.size} frames")

            for ((batchIndex, batch) in batches.withIndex()) {
                Log.d("FaceVerification", "Processing batch ${batchIndex + 1}/${batches.size}")

                val framesBase64 = batch.map { frame ->
                    bitmapToBase64(frame.bitmap, 50) // Reduced quality parameter
                }

                val metadata = createBatchMetadata(
                    sessionId = sessionId,
                    userId = userId,
                    timestamp = startTime,
                    captureCompletedAt = captureCompletedAt,
                    batchIndex = batchIndex,
                    totalBatches = batches.size,
                    originalFrames = frameData,
                    processedFrames = processedFrames,
                    selectedFrames = batch
                )

                val request = FaceVerificationRequest(
                    sessionId = sessionId,
                    metadata = metadata,
                    framesBase64 = framesBase64
                )

                val token = getAuthToken().first()
                if (token.isNullOrEmpty()) {
                    return Result.failure(Exception("No auth token available"))
                }

                val response = faceVerificationApi.submitFaceVerification("Bearer $token", request)

                if (response.isSuccessful && response.body() != null) {
                    val verificationResponse = response.body()!!
                    if (!verificationResponse.success) {
                        return Result.failure(Exception("Batch ${batchIndex + 1} failed: ${verificationResponse.message}"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.message
                    } catch (e: Exception) {
                        "Batch ${batchIndex + 1} failed: ${response.message()}"
                    }
                    return Result.failure(Exception(errorMessage))
                }

                Log.d("FaceVerification", "Batch ${batchIndex + 1} sent successfully")
            }

            Result.success("Face verification completed successfully")

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
            Log.d("FaceVerification", "Video duration: $duration ms")

            val frameRate = 30 // Assume 30fps
            val intervalMs = 200L // Extract frame every 200ms (5 fps)

            var currentTime = 0L
            var frameIndex = 0

            while (currentTime < duration) {
                try {
                    val bitmap = retriever.getFrameAtTime(currentTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        Log.d("FaceVerification", "Extracted frame $frameIndex at $currentTime ms")
                        frames.add(FrameData(
                            bitmap = bitmap,
                            timestampMs = currentTime,
                            frameIndex = frameIndex++
                        ))
                    } else {
                        Log.w("FaceVerification", "Failed to extract bitmap at $currentTime ms")
                    }
                } catch (e: Exception) {
                    Log.w("FaceVerificationRepo", "Failed to extract frame at $currentTime ms", e)
                }

                currentTime += intervalMs
            }

            Log.d("FaceVerification", "Total frames extracted: ${frames.size}")

        } finally {
            retriever.release()
        }

        return frames
    }

    private suspend fun processFramesWithFaceDetection(frames: List<FrameData>): List<ProcessedFrame> {
        val processedFrames = mutableListOf<ProcessedFrame>()

        Log.d("FaceVerification", "Processing ${frames.size} frames for face detection")

        for (frame in frames) {
            try {
                val inputImage = InputImage.fromBitmap(frame.bitmap, 0)
                val faces = detector.process(inputImage).await()

                Log.d("FaceVerification", "Frame ${frame.frameIndex}: detected ${faces.size} faces")

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

        Log.d("FaceVerification", "Processed frames with faces: ${processedFrames.size}")
        return processedFrames
    }

    private fun selectBestFrames(processedFrames: List<ProcessedFrame>): List<ProcessedFrame> {
        Log.d("FaceVerification", "Selecting from ${processedFrames.size} processed frames")

        val filtered = processedFrames.filter { !it.isBlurry && it.faceConfidence > 0.57f }
        Log.d("FaceVerification", "After filtering: ${filtered.size} frames")

        val selected = filtered
            .sortedByDescending { it.faceConfidence }
            .take(10) // Select top 10 frames
            .sortedBy { it.frameData.timestampMs } // Sort back by timestamp

        Log.d("FaceVerification", "Final selected frames: ${selected.size}")
        return selected
    }

    private fun createBatchMetadata(
        sessionId: String,
        userId: String,
        timestamp: Long,
        captureCompletedAt: Long,
        batchIndex: Int,
        totalBatches: Int,
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
            frameSelectionCriteria = "confidence_and_quality_based_batch_${batchIndex + 1}_of_$totalBatches",
            averageFaceConfidence = confidences.average().toFloat(),
            minFaceConfidence = confidences.minOrNull() ?: 0f,
            maxFaceConfidence = confidences.maxOrNull() ?: 0f,
            averageFaceSize = faceSizes.average().toFloat(),
            lightingQuality = assessOverallLightingQuality(processedFrames),
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            cameraResolution = "320x240", // Updated to reflect resized images
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
            compressionQuality = 50
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
        // Resize bitmap to smaller dimensions (320x240)
        val resizedBitmap = resizeBitmap(bitmap, 320, 240)

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream) // Reduced quality to 50
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
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