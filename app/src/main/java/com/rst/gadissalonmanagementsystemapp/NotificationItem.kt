package com.rst.gadissalonmanagementsystemapp

data class NotificationItem(
    val title: String,
    val body: String,
    val timeAgo: String,
    val iconResId: Int = R.drawable.ic_notifications // Default icon
)