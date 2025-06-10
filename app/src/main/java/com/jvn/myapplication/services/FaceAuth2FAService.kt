package com.jvn.myapplication.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.jvn.myapplication.config.ApiConfig
import com.jvn.myapplication.data.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * ## 2FA Face Authentication Service
 * 
 * ### Overview
 * This service implements the mobile app side of a 2FA face authentication workflow using Firebase FCM.
 * When a user tries to log in on a web browser, the backend sends an FCM notification to their mobile app
 * requesting face verification. The user captures their face image which is sent to the backend for validation.
 * 
 * ### Complete Workflow
 * 1. **Web Initiates 2FA**: Browser calls `POST /api/auth/2fa/face/login/web`
 * 2. **Backend Sends FCM**: Backend generates requestId and sends FCM to mobile app
 * 3. **Mobile Receives FCM**: App shows FaceAuth2FAScreen with camera
 * 4. **Face Capture**: User's face is captured via CameraX
 * 5. **Send to Backend**: Face image sent to `POST /api/auth/2fa/face/complete/{requestId}`
 * 6. **Backend Validates**: Backend calls face recognition microservice
 * 7. **WebSocket Notification**: Result sent to web browser via WebSocket
 * 
 * ### Setup Instructions
 * 1. **Firebase Configuration**: 
 *    - Replace `google-services.json` with your Firebase project config
 *    - Ensure FCM is enabled in Firebase Console
 * 
 * 2. **Backend URL Configuration**:
 *    - Update `BASE_URL` constant below with your actual backend URL
 *    - Ensure backend implements the required endpoints:
 *      - `POST /api/auth/2fa/face/complete/{requestId}`
 *      - `POST /api/auth/2fa/face/deny/{requestId}`
 *      - `GET /api/auth/2fa/face/status/{requestId}`
 * 
 * 3. **FCM Payload Format**:
 *    Backend should send FCM with this data structure:
 *    ```json
 *    {
 *      "type": "face_auth_request",
 *      "requestId": "req_1234567890_abcdef123",
 *      "title": "Face Authentication Required",
 *      "body": "Login request for user@example.com. Please verify your identity.",
 *      "timestamp": "2024-01-01T00:00:00.000Z"
 *    }
 *    ```
 * 
 * ### Usage Example
 * ```kotlin
 * val faceAuth2FAService = FaceAuth2FAService(context)
 * 
 * // When user completes face capture
 * val result = faceAuth2FAService.completeFaceAuth(requestId, faceImageFile)
 * when (result) {
 *     is FaceAuthResult.Success -> {
 *         // Show success message, auth completed
 *     }
 *     is FaceAuthResult.Error -> {
 *         // Show error message, retry or cancel
 *     }
 * }
 * ```
 * 
 * ### Security Considerations
 * - Face images are sent over HTTPS only
 * - Images are temporarily stored and deleted after processing
 * - Requests have a 5-minute timeout
 * - Authentication tokens are required for API calls
 * 
 * @author Your Name
 * @since 1.0.0
 */
class FaceAuth2FAService(private val context: Context) {

    companion object {
        private const val TAG = "FaceAuth2FA"
    }

    private val authRepository = AuthRepository(context)
    private val gson = Gson()
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    /**
     * Complete 2FA face authentication by sending face image to backend
     * 
     * This corresponds to Step 8 in the workflow diagram:
     * POST /api/auth/2fa/face/complete/{requestId}
     * 
     * @param requestId The unique request ID received from FCM notification
     * @param faceImageFile The captured face image file
     * @return True if authentication was successful, false otherwise
     */
    suspend fun completeFaceAuth(requestId: String, faceImageFile: File): FaceAuthResult {
        return try {
            Log.d(TAG, "🔐 Starting face auth completion for request: $requestId")
            Log.d(TAG, "🌐 API Base URL: ${ApiConfig.API_BASE_URL}")
            Log.d(TAG, "🔧 Development Mode: ${ApiConfig.DEVELOPMENT_MODE}")
            
            // Get authentication token (if needed for backend auth)
            val authToken = authRepository.getAuthToken().first()
            
            // Create multipart request body with face image
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "face_image",
                    faceImageFile.name,
                    faceImageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            // Build the request
            val request = Request.Builder()
                .url("${ApiConfig.API_BASE_URL}/api/auth/2fa/face/complete/$requestId")
                .post(requestBody)
                .apply {
                    // Add auth header if token is available
                    authToken?.let { token ->
                        addHeader("Authorization", "Bearer $token")
                    }
                    // Add ngrok header to bypass browser warning
                    if (ApiConfig.API_BASE_URL.contains("ngrok")) {
                        addHeader("ngrok-skip-browser-warning", "true")
                    }
                }
                .addHeader("Content-Type", "multipart/form-data")
                .build()

            Log.d(TAG, "📡 Sending face image to backend...")
            Log.d(TAG, "🎯 URL: ${request.url}")
            Log.d(TAG, "📁 File size: ${faceImageFile.length()} bytes")

            // Execute the request on IO thread
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }.use { response ->
                val responseBody = response.body?.string()
                
                Log.d(TAG, "📨 Response code: ${response.code}")
                Log.d(TAG, "📋 Response body: $responseBody")

                when (response.code) {
                    200 -> {
                        Log.d(TAG, "✅ Face authentication successful!")
                        FaceAuthResult.Success("Face authentication completed successfully")
                    }
                    400 -> {
                        Log.e(TAG, "❌ Bad request - Invalid face image or request")
                        FaceAuthResult.Error("Invalid face image or request format")
                    }
                    401 -> {
                        Log.e(TAG, "❌ Unauthorized - Authentication failed")
                        FaceAuthResult.Error("Authentication failed")
                    }
                    404 -> {
                        Log.e(TAG, "❌ Request not found or expired")
                        FaceAuthResult.Error("Authentication request not found or expired")
                    }
                    422 -> {
                        Log.e(TAG, "❌ Face verification failed")
                        FaceAuthResult.Error("Face verification failed - please try again")
                    }
                    else -> {
                        Log.e(TAG, "❌ Unexpected response: ${response.code}")
                        FaceAuthResult.Error("Unexpected error occurred")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "🌐 Network error during face auth", e)
            FaceAuthResult.Error("Network error - please check your connection")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Unexpected error during face auth", e)
            FaceAuthResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Cancel/Deny a face authentication request
     * This can be called if user declines the face auth request
     * 
     * @param requestId The unique request ID to cancel
     */
    suspend fun denyFaceAuth(requestId: String): Boolean {
        return try {
            Log.d(TAG, "❌ Denying face auth request: $requestId")
            
            val authToken = authRepository.getAuthToken().first()
            
            val request = Request.Builder()
                .url("${ApiConfig.API_BASE_URL}/api/auth/2fa/face/deny/$requestId")
                .post(okhttp3.RequestBody.create("".toMediaTypeOrNull(), ""))
                .apply {
                    authToken?.let { token ->
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()

            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }.use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Face auth request denied successfully")
                    true
                } else {
                    Log.e(TAG, "❌ Failed to deny face auth request: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error denying face auth request", e)
            false
        }
    }

    /**
     * Check if a face authentication request is still valid
     * 
     * @param requestId The request ID to check
     * @return True if request is still valid, false if expired
     */
    suspend fun isRequestValid(requestId: String): Boolean {
        return try {
            val authToken = authRepository.getAuthToken().first()
            
            val request = Request.Builder()
                .url("${ApiConfig.API_BASE_URL}/api/auth/2fa/face/status/$requestId")
                .get()
                .apply {
                    authToken?.let { token ->
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()

            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }.use { response ->
                response.code == 200
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking request validity", e)
            false
        }
    }
}

/**
 * Result wrapper for face authentication operations
 */
sealed class FaceAuthResult {
    data class Success(val message: String) : FaceAuthResult()
    data class Error(val message: String) : FaceAuthResult()
} 