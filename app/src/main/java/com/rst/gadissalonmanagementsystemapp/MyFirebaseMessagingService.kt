package com.rst.gadissalonmanagementsystemapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: $data")

            // Parse data from the 'data' map
            val title = data["title"] ?: "New Notification"
            val body = data["body"] ?: ""

            // Get the IDs for the actions
            val bookingId = data["bookingId"]
            val orderId = data["orderId"]
            val ticketId = data["ticketId"] // For support tickets

            sendNotification(title, body, bookingId, orderId, ticketId)

        } else {
            // Fallback: Handle if it's just a 'notification' payload (no data)
            remoteMessage.notification?.let {
                Log.d(TAG, "Notification Body: ${it.body}")
                sendNotification(
                    it.title ?: "New Notification",
                    it.body ?: "",
                    null,
                    null,
                    null
                )
            }
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

    private fun sendNotification(
        title: String,
        messageBody: String,
        bookingId: String?,
        orderId: String?,
        ticketId: String? // Added ticketId
    ) {

        val intent = Intent(this, CustomerMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Pass the IDs as extras so the activity knows where to navigate
            if (bookingId != null) {
                putExtra("bookingId", bookingId)
                Log.d(TAG, "Creating PendingIntent with bookingId: $bookingId")
            }
            if (orderId != null) {
                putExtra("orderId", orderId)
                Log.d(TAG, "Creating PendingIntent with orderId: $orderId")
            }
            if (ticketId != null) {
                putExtra("ticketId", ticketId)
                Log.d(TAG, "Creating PendingIntent with ticketId: $ticketId")
            }
        }

        // 2. Create the PendingIntent
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt() /* Unique request code */,
            intent,
            pendingIntentFlag
        )
        // --- END PENDING INTENT LOGIC ---

        val channelId = "gadis_salon_default_channel"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // This ensures it pops up
            .setContentIntent(pendingIntent) // <-- ADD THIS to make it clickable

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gadis Salon General Notifications",
                NotificationManager.IMPORTANCE_HIGH // High importance allows sound and pop-ups
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
