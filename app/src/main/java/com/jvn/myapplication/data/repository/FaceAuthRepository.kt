package com.jvn.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jvn.myapplication.data.api.FaceAuthApiService
import com.jvn.myapplication.data.api.FaceAuthResponse
import com.jvn.myapplication.data.api.FaceStatusResponse
import com.jvn.myapplication.data.api.FaceVerifyResponse
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class FaceAuthRepository(private val context: Context) {
    private val faceAuthApi: FaceAuthApiService = NetworkModule.faceAuthApi
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    suspend fun registerFaceWithVideo(videoUri: Uri, userId: String): Result<String> {
        return try {
            Log.d("FaceAuth", "Processing 10-second video for registration: $videoUri")

            // Extract every 5th frame from 10-second video
            val frames = extractEveryFifthFrame(videoUri)
            
            if (frames.isEmpty()) {
                return Result.failure(Exception("No frames could be extracted from video"))
            }

            Log.d("FaceAuth", "Extracted ${frames.size} frames for registration")

            // Convert frames to multipart files
            val imageParts = frames.mapIndexed { index, bitmap ->
                val byteArray = bitmapToJpegBytes(bitmap)
                val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", "frame_$index.jpg", requestBody)
            }

            // Send to backend
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No auth token available"))
            }

            val response = faceAuthApi.registerFace("Bearer $token", imageParts)

            if (response.isSuccessful && response.body() != null) {
                val faceAuthResponse = response.body()!!
                if (faceAuthResponse.success) {
                    Result.success("Face registration started. Training ${frames.size} images...")
                } else {
                    Result.failure(Exception(faceAuthResponse.message))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception("Registration failed: $errorMessage"))
            }

        } catch (e: Exception) {
            Log.e("FaceAuth", "Error during face registration", e)
            Result.failure(e)
        }
    }

    suspend fun verifyFace(imageUri: Uri): Result<FaceVerifyResponse> {
        return try {
            Log.d("FaceAuth", "Verifying face with image: $imageUri")

            // Convert image to multipart
            val bitmap = uriToBitmap(imageUri)
            val byteArray = bitmapToJpegBytes(bitmap)
            val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "verification.jpg", requestBody)

            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No auth token available"))
            }

            val response = faceAuthApi.verifyFace("Bearer $token", imagePart)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Verification failed"
                Result.failure(Exception("Verification failed: $errorMessage"))
            }

        } catch (e: Exception) {
            Log.e("FaceAuth", "Error during face verification", e)
            Result.failure(e)
        }
    }

    suspend fun getTrainingStatus(): Result<FaceStatusResponse> {
        return try {
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No auth token available"))
            }

            val response = faceAuthApi.getStatus("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Status check failed"
                Result.failure(Exception("Status check failed: $errorMessage"))
            }

        } catch (e: Exception) {
            Log.e("FaceAuth", "Error checking training status", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFaceData(): Result<String> {
        return try {
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No auth token available"))
            }

            val response = faceAuthApi.deleteFaceData("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val faceAuthResponse = response.body()!!
                if (faceAuthResponse.success) {
                    Result.success("Face data deleted successfully")
                } else {
                    Result.failure(Exception(faceAuthResponse.message))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Deletion failed"
                Result.failure(Exception("Deletion failed: $errorMessage"))
            }

        } catch (e: Exception) {
            Log.e("FaceAuth", "Error deleting face data", e)
            Result.failure(e)
        }
    }

    private fun extractEveryFifthFrame(videoUri: Uri): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()

        try {
            retriever.setDataSource(context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Log.d("FaceAuth", "Video duration: $duration ms")

            // For 10-second video at 30fps = 300 total frames
            // Every 5th frame = 300/5 = 60 frames
            val frameRate = 30 // Assume 30fps
            val totalFrames = ((duration / 1000.0) * frameRate).toInt()
            
            Log.d("FaceAuth", "Total estimated frames: $totalFrames")

            var frameIndex = 0
            while (frameIndex < totalFrames) {
                try {
                    // Only extract every 5th frame
                    if (frameIndex % 5 == 0) {
                        val timeUs = (frameIndex * 1000000L) / frameRate // Convert to microseconds
                        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        
                        if (bitmap != null) {
                            frames.add(bitmap)
                            Log.d("FaceAuth", "Extracted frame ${frames.size} at index $frameIndex (${timeUs}μs)")
                        }
                    }
                    frameIndex++
                } catch (e: Exception) {
                    Log.w("FaceAuth", "Failed to extract frame at index $frameIndex", e)
                    frameIndex++
                }
            }

            Log.d("FaceAuth", "Total frames extracted: ${frames.size}")

        } catch (e: Exception) {
            Log.e("FaceAuth", "Error extracting frames from video", e)
        } finally {
            retriever.release()
        }

        return frames
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return android.graphics.BitmapFactory.decodeStream(inputStream)
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun getAuthToken() = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
} 