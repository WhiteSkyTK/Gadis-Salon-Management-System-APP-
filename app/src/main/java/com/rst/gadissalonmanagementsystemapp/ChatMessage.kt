package com.rst.gadissalonmanagementsystemapp

data class ChatMessage(
    val messageId: String,
    val bookingId: String, // Links the chat to a specific booking
    val senderId: String, // Who sent the message (e.g., customer ID or stylist ID)
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val isSentByUser: Boolean // To easily align left/right in the UI
)