package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val productId: String = "",
    val name: String = "",
    val size: String = "",
    val price: Double = 0.0,
    var quantity: Int = 0,
    val imageUrl: String = "",
    val stock: Int = 0
) : Parcelable