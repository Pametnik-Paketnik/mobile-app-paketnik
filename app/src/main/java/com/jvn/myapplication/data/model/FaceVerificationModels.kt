package com.jvn.myapplication.data.model

import android.graphics.RectF

data class FaceVerificationRequest(
    val sessionId: String,
    val metadata: FaceVerificationMetadata,
    val framesBase64: List<String> // Base64 encoded JPEG frames
)

data class FaceVerificationResponse(
    val success: Boolean,
    val message: String,
    val verificationId: String? = null
)

data class FaceVerificationMetadata(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val captureCompletedAt: Long,
    val totalCaptureTimeMs: Long,
    val originalFrameCount: Int,
    val selectedFrameCount: Int,
    val frameSelectionCriteria: String,
    val averageFaceConfidence: Float,
    val minFaceConfidence: Float,
    val maxFaceConfidence: Float,
    val averageFaceSize: Float,
    val lightingQuality: String,
    val deviceModel: String,
    val androidVersion: String,
    val cameraResolution: String,
    val orientationChanges: Int,
    val frameMetrics: List<FrameMetric>
)

data class FrameMetric(
    val frameIndex: Int,
    val timestampMs: Long,
    val faceConfidence: Float,
    val faceBounds: FaceBounds,
    val headPose: HeadPose,
    val eyesOpen: Boolean,
    val isBlurry: Boolean,
    val compressionQuality: Int
)

data class FaceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class HeadPose(
    val pitch: Float,
    val yaw: Float,
    val roll: Float
)