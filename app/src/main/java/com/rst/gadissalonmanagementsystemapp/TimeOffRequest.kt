package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

// Data class to represent a Time Off Request
// @Parcelize allows us to pass this object between fragments
@Parcelize
data class TimeOffRequest(
    var id: String = "",
    val stylistId: String = "",
    val stylistName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, approved, rejected
    @ServerTimestamp
    val timestamp: Date? = null
) : Parcelable
