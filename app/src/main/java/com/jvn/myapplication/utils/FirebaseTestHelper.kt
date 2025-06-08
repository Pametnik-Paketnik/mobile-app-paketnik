package com.jvn.myapplication.utils

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FirebaseTestHelper {
    
    private const val TAG = "FirebaseTest"
    
    suspend fun testFirebaseSetup(context: Context): Boolean {
        return try {
            Log.d(TAG, "🔥 Testing Firebase setup...")
            
            // Test FCM token generation
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ FCM Token generated successfully!")
            Log.d(TAG, "📱 Token: ${token.take(20)}...")
            
            // Test token refresh
            FirebaseMessaging.getInstance().deleteToken().await()
            val newToken = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ Token refresh successful!")
            Log.d(TAG, "🔄 New Token: ${newToken.take(20)}...")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase setup failed: ${e.message}", e)
            false
        }
    }
    
    fun logFirebaseInfo() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "❌ Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "🎯 Current FCM Token: $token")
            Log.d(TAG, "📋 Copy this token for backend testing")
        }
    }
}