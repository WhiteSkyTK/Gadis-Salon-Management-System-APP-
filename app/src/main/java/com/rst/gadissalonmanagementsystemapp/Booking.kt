package com.rst.gadissalonmanagementsystemapp

data class Booking(
    val styleName: String,
    val stylistName: String,
    val date: String,
    val time: String,
    val status: String,
    val imageResId: Int = R.drawable.ic_placeholder_image
)