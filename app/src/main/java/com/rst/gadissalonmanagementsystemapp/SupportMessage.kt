package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // Add this
data class SupportMessage(
    var id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "New",
    val participantUids: List<String> = emptyList()
) : Parcelable // And implement this