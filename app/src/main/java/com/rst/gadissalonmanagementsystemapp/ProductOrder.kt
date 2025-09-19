package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductOrder(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "Pending Pickup" // e.g., "Pending Pickup", "Completed"
) : Parcelable