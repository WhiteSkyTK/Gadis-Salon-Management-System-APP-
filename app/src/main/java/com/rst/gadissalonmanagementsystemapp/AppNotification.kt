package com.rst.gadissalonmanagementsystemapp

// Represents a single notification for a user
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val bookingId: String? = null,
    val orderId: String? = null,
    val ticketId: String? = null
)