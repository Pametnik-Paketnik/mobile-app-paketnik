package com.jvn.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jvn.myapplication.data.repository.AuthRepository

class LoginApprovalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val pendingAuthId = inputData.getString("pendingAuthId") ?: return Result.failure()
            val action = inputData.getString("action") ?: return Result.failure()
            val authRepository = AuthRepository(applicationContext)
            
            when (action) {
                "approve" -> {
                    authRepository.approvePendingAuth(pendingAuthId)
                    Log.d("LoginApprovalWorker", "Login approved successfully")
                }
                "deny" -> {
                    authRepository.denyPendingAuth(pendingAuthId)
                    Log.d("LoginApprovalWorker", "Login denied successfully")
                }
                else -> return Result.failure()
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("LoginApprovalWorker", "Failed to process login approval", e)
            Result.retry()
        }
    }
} 