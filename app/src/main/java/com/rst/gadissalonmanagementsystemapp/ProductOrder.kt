package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class ProductOrder(
    var id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    @ServerTimestamp
    val timestamp: Date? = null,
    var status: String = "Pending Pickup"
) : Parcelable