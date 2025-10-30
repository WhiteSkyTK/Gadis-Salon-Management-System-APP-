package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class to represent an income record from Firestore
data class IncomeRecord(
    val amount: Double = 0.0,
    @ServerTimestamp
    val createdAt: Date? = null,
    val type: String = "", // "booking" or "order"
    val serviceName: String = "",
    val stylistName: String = "",
    val customerName: String = "",
    val bookingId: String? = null,
    val orderId: String? = null
)
