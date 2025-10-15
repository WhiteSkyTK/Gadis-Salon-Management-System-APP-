package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminBooking(
    var id: String = "",
    val hairstyleId: String = "",
    val customerId: String = "",
    val stylistId: String = "",
    val serviceName: String = "",
    val customerName: String = "",
    val stylistName: String = "",
    val date: String = "",
    val time: String = "",
    var status: String = "Pending",
    var workerUnreadCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cancellationReason: String = "",
    val declinedBy: List<String> = emptyList()
) : Parcelable
