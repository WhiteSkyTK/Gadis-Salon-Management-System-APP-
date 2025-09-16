package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.firestore.Exclude

// This is the final data class for all chat messages
data class ChatMessage(
    val messageId: String = "",
    val bookingId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,

    // This is a temporary flag for the UI. The @get:Exclude annotation
    // tells Firestore to completely ignore this field when saving/loading.
    @get:Exclude var isSentByUser: Boolean = false
)
