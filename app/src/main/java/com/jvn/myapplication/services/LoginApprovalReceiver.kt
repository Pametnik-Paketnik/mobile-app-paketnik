package com.jvn.myapplication.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jvn.myapplication.MainActivity
import com.jvn.myapplication.workers.LoginApprovalWorker

class LoginApprovalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingAuthId = intent.getStringExtra("pendingAuthId") ?: return
        
        when (intent.action) {
            "APPROVE_LOGIN" -> {
                // Start face verification directly
                val approveIntent = Intent(context, MainActivity::class.java).apply {
                    action = "LOGIN_APPROVAL_FACE_VERIFICATION" 
                    putExtra("pendingAuthId", pendingAuthId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                
                context.startActivity(approveIntent)
            }
            "DENY_LOGIN" -> {
                // Deny the login request immediately
                val denyWork = OneTimeWorkRequestBuilder<LoginApprovalWorker>()
                    .setInputData(workDataOf(
                        "pendingAuthId" to pendingAuthId,
                        "action" to "deny"
                    ))
                    .build()
                
                WorkManager.getInstance(context).enqueue(denyWork)
                Toast.makeText(context, "Login request denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 