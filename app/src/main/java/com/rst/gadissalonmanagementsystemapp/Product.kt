package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// This nested class represents a single version of a product (e.g., a specific size)
@Parcelize
data class ProductVariant(
    val size: String,
    val price: Double,
    val priceOld: Double? = null // Optional older price for showing discounts
) : Parcelable

@Parcelize
data class Product(
    override val id: String, // Add 'override'
    override val name: String, // Add 'override'
    val reviews: String,
    val variants: List<ProductVariant>,
    val imageResId: Int = R.drawable.ic_placeholder_image
) : Parcelable, Favoritable // Implement Favoritable