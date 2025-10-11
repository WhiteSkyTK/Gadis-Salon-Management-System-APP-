package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize


@Parcelize // Add this
data class SupportMessage(
    var id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val message: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val status: String = "New",
    val participantUids: List<String> = emptyList()
) : Parcelable // And implement this