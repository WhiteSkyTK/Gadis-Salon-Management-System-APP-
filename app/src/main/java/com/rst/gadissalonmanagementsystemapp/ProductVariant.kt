package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// This nested class represents a single version of a product (e.g., a specific size)
@Parcelize
data class ProductVariant(
    val size: String = "",
    val price: Double = 0.0,
    val priceOld: Double? = null,
    val stock: Int = 0
) : Parcelable