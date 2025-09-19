package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminBooking(
    var id: String = "",
    val customerId: String = "",
    val serviceName: String = "",
    val customerName: String = "",
    val stylistName: String = "",
    val date: String = "",
    val time: String = "",
    var status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
