package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val name: String,
    val detail: String,
    val type: String,
    val imageResId: Int = R.drawable.ic_placeholder_image
) : Parcelable {
    companion object {
        const val TYPE_PRODUCT = "product"
        const val TYPE_HAIRSTYLE = "hairstyle"
    }
}