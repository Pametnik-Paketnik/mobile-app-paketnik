package com.jvn.myapplication.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jvn.myapplication.MainActivity
import com.jvn.myapplication.R
import com.jvn.myapplication.workers.TokenUpdateWorker

/**
 * Firebase Cloud Messaging Service for 2FA Face Authentication
 * 
 * This service handles:
 * 1. FCM token updates to backend
 * 2. 2FA face authentication requests from backend
 * 3. Notification display for face verification
 */
class PaketnikFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_2FA_Service"
        private const val CHANNEL_ID = "face_auth_channel"
        private const val FACE_AUTH_NOTIFICATION_ID = 200
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "🔥 2FA FCM Service initialized")
    }

    /**
     * Called when FCM token is refreshed
     * This token must be sent to backend for 2FA notifications
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "📱 New FCM token generated: ${token.take(20)}...")
        
        // Register device with backend using WorkManager (handles network retry)
        val deviceRegistrationWork = OneTimeWorkRequestBuilder<com.jvn.myapplication.workers.DeviceRegistrationWorker>()
            .setInputData(workDataOf(
                com.jvn.myapplication.workers.DeviceRegistrationWorker.FCM_TOKEN_KEY to token
            ))
            .build()
        
        WorkManager.getInstance(this).enqueue(deviceRegistrationWork)
        Log.d(TAG, "⬆️ Device registration work scheduled")
    }

    /**
     * Handle incoming FCM messages
     * Expected message types:
     * - face_auth_request: 2FA face authentication request from backend
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "📨 Message received from: ${remoteMessage.from}")
        Log.d(TAG, "📋 Message data: ${remoteMessage.data}")
        
        val messageType = remoteMessage.data["type"]
        
        when (messageType) {
            "face_auth_request" -> handleFaceAuthRequest(remoteMessage)
            else -> {
                Log.w(TAG, "⚠️ Unknown message type: $messageType")
                showDefaultNotification(remoteMessage)
            }
        }
    }

    /**
     * Handle 2FA face authentication request
     * Expected payload from backend:
     * {
     *   "notification": {
     *     "title": "Face Authentication Required",
     *     "body": "Login request for user@example.com. Please verify your identity."
     *   },
     *   "data": {
     *     "type": "face_auth_request",
     *     "requestId": "req_1234567890_abcdef123",
     *     "timestamp": "2024-01-01T00:00:00.000Z"
     *   }
     * }
     */
    private fun handleFaceAuthRequest(remoteMessage: RemoteMessage) {
        val requestId = remoteMessage.data["requestId"]
        val timestamp = remoteMessage.data["timestamp"]
        
        // Use notification title/body if available, otherwise fall back to data or defaults
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "🔐 Face Authentication Required"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "Please verify your identity to complete login"

        if (requestId.isNullOrEmpty()) {
            Log.e(TAG, "❌ Missing requestId in face auth request")
            return
        }

        Log.d(TAG, "🔐 Processing face auth request: $requestId")
        Log.d(TAG, "📋 Title: $title")
        Log.d(TAG, "📋 Body: $body")
        Log.d(TAG, "📋 Timestamp: $timestamp")

        // Create intent to open app with face verification screen
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "FACE_AUTH_REQUEST"
            putExtra("requestId", requestId)
            putExtra("timestamp", timestamp)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create high-priority notification for immediate attention
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$body\n\nTap to open the app and complete face verification.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setTimeoutAfter(5 * 60 * 1000) // 5 minutes timeout
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FACE_AUTH_NOTIFICATION_ID, notification)
        
        Log.d(TAG, "🔔 Face auth notification displayed for request: $requestId")
    }

    /**
     * Handle other notification types or fallback notifications
     */
    private fun showDefaultNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Paketnik"
        val body = remoteMessage.notification?.body ?: "You have a new notification"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Create notification channel for face authentication notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Face Authentication",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for 2FA face authentication requests"
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "📢 Notification channel created: $CHANNEL_ID")
        }
    }
} 