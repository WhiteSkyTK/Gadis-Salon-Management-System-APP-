package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminBooking(
    var id: String = "",
    val hairstyleId: String = "",
    val customerId: String = "",
    var stylistId: String = "",
    val serviceName: String = "",
    val customerName: String = "",
    var stylistName: String = "",
    var date: String = "",
    var time: String = "",
    var status: String = "Pending",
    var workerUnreadCount: Int = 0,
    val timestamp: Long = 0L,
    val cancellationReason: String = "",
    val declineReason: String = "",
    val declinedBy: List<String> = emptyList()
) : Parcelable
