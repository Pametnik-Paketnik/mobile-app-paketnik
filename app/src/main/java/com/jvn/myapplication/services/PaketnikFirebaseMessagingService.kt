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
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.workers.TokenUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaketnikFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "login_approval_channel"
        private const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Use WorkManager to update token on backend (handles network retry)
        val updateTokenWork = OneTimeWorkRequestBuilder<TokenUpdateWorker>()
            .setInputData(workDataOf("fcm_token" to token))
            .build()
        
        WorkManager.getInstance(this).enqueue(updateTokenWork)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Handle different types of notifications
        val messageType = remoteMessage.data["type"]
        
        when (messageType) {
            "login_approval" -> handleLoginApprovalNotification(remoteMessage)
            else -> {
                // Handle other notification types or show default notification
                showDefaultNotification(remoteMessage)
            }
        }
    }

    private fun handleLoginApprovalNotification(remoteMessage: RemoteMessage) {
        val pendingAuthId = remoteMessage.data["pendingAuthId"] ?: return
        val username = remoteMessage.data["username"] ?: "Unknown user"
        val ip = remoteMessage.data["ip"] ?: "Unknown IP"
        val location = remoteMessage.data["location"] ?: "Unknown location"

        // Create intent to open app with login approval screen
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "LOGIN_APPROVAL"
            putExtra("pendingAuthId", pendingAuthId)
            putExtra("username", username)
            putExtra("ip", ip)
            putExtra("location", location)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            pendingAuthId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create approve action
        val approveIntent = Intent(this, LoginApprovalReceiver::class.java).apply {
            action = "APPROVE_LOGIN"
            putExtra("pendingAuthId", pendingAuthId)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            this,
            (pendingAuthId + "_approve").hashCode(),
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create deny action
        val denyIntent = Intent(this, LoginApprovalReceiver::class.java).apply {
            action = "DENY_LOGIN"
            putExtra("pendingAuthId", pendingAuthId)
        }
        val denyPendingIntent = PendingIntent.getBroadcast(
            this,
            (pendingAuthId + "_deny").hashCode(),
            denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔐 Login Approval Required")
            .setContentText("$username is trying to log in from $ip")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$username is trying to log in from $ip ($location). Tap to verify your identity with face recognition.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SECURITY)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Approve",
                approvePendingIntent
            )
            .addAction(
                R.drawable.ic_close,
                "Deny",
                denyPendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Login Approval",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for login approval requests"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 