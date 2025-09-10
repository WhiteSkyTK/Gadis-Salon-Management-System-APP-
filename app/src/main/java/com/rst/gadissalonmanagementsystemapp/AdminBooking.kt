package com.rst.gadissalonmanagementsystemapp

data class AdminBooking(
    val id: String = "",
    val serviceName: String = "",
    val customerName: String = "",
    val stylistName: String = "",
    val date: String = "",
    val time: String = "",
    var status: String = "Pending" // "Pending", "Confirmed", "Completed", "Cancelled"
)