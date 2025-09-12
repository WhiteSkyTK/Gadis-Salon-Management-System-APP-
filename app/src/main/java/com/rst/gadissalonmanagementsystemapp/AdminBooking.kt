package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.Timestamp

// This is the main data class for all bookings in the app
data class AdminBooking(
    var id: String = "", // var so Firestore can set it
    val serviceName: String = "",
    val customerName: String = "",
    val stylistName: String = "",
    val date: String = "",
    val time: String = "",
    var status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)
