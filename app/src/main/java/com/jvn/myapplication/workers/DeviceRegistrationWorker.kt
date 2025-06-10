package com.jvn.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.first

/**
 * Worker to handle device registration for 2FA notifications
 * This worker will attempt to register the device with the backend
 * and can be retried if it fails (e.g., due to network issues)
 */
class DeviceRegistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DeviceRegistrationWorker"
        const val FCM_TOKEN_KEY = "fcm_token"
    }

    override suspend fun doWork(): Result {
        return try {
            val fcmToken = inputData.getString(FCM_TOKEN_KEY) ?: return Result.failure()
            val authRepository = AuthRepository(applicationContext)
            
            Log.d(TAG, "🔐 Starting device registration...")
            
            // Check if user is authenticated
            val authToken = authRepository.getAuthToken().first()
            if (authToken.isNullOrEmpty()) {
                Log.d(TAG, "❌ User not authenticated, cannot register device")
                return Result.failure()
            }
            
            // Register device with backend
            val result = authRepository.registerDevice(fcmToken)
            
            result.onSuccess { deviceResponse ->
                Log.d(TAG, "✅ Device registration successful: ${deviceResponse.message}")
                Result.success()
            }.onFailure { exception ->
                Log.e(TAG, "❌ Device registration failed: ${exception.message}")
                // Retry if it's a network error, otherwise fail permanently
                if (exception.message?.contains("network", ignoreCase = true) == true ||
                    exception.message?.contains("timeout", ignoreCase = true) == true) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            
            result.fold(
                onSuccess = { Result.success() },
                onFailure = { 
                    // Check if we should retry
                    if (it.message?.contains("network", ignoreCase = true) == true ||
                        it.message?.contains("timeout", ignoreCase = true) == true) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 Unexpected error during device registration", e)
            Result.retry()
        }
    }
} 