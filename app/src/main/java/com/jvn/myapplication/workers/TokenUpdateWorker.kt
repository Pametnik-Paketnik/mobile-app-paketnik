package com.jvn.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jvn.myapplication.data.repository.AuthRepository

class TokenUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val fcmToken = inputData.getString("fcm_token") ?: return Result.failure()
            val authRepository = AuthRepository(applicationContext)
            
            // Update FCM token on backend
            authRepository.updateFcmToken(fcmToken)
            
            Log.d("TokenUpdateWorker", "FCM token updated successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("TokenUpdateWorker", "Failed to update FCM token", e)
            Result.retry()
        }
    }
} 