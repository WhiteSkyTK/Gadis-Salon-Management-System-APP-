package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// This is the final data class for all chat messages
data class ChatMessage(
    val bookingId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val messageText: String = "",
    var status: String = "SENT",
    @ServerTimestamp val timestamp: Date? = null,
    @get:Exclude var isSentByUser: Boolean = false
)
