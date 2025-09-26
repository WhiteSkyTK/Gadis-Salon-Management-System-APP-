package com.rst.gadissalonmanagementsystemapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_Service"

    // This is called when a new notification is received while the app is in the foreground
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "New message from: ${remoteMessage.from}")

        // Check if the message contains a notification payload and display it locally.
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification Body: ${it.body}")
            sendNotification(it.title ?: "New Notification", it.body ?: "")
        }
    }

    // This is called when a new token is generated for the device
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendTokenToFirestore(token)
    }

    private fun sendTokenToFirestore(token: String) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            Log.d(TAG, "Attempting to save FCM token for user UID: $uid")
            // We use a background coroutine because this can be called at any time,
            // even when there's no screen visible.
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.updateUserFCMToken(uid, token)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "gadis_salon_default_channel"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification) // Your white-on-transparent icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensures the notification pops up

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // --- THIS IS THE CRUCIAL PART for Android 8.0 (Oreo) and above ---
        // We must create a Notification Channel for sounds and pop-ups to work on modern devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gadis Salon General Notifications",
                NotificationManager.IMPORTANCE_HIGH // High importance allows sound and pop-ups
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt() /* Unique ID for each notification */, notificationBuilder.build())
    }
}
