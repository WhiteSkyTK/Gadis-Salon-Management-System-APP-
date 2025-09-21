package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.firestore.Exclude

// This is the final data class for all chat messages
data class ChatMessage(
    val bookingId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    var status: String = "SENT",
    // This is a temporary flag for the UI. The @get:Exclude annotation
    // tells Firestore to completely ignore this field when saving/loading.
    @get:Exclude var isSentByUser: Boolean = false
)
