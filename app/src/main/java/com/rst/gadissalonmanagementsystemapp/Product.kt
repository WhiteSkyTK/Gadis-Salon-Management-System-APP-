package com.rst.gadissalonmanagementsystemapp

data class Product(
    val name: String,
    val detail: String, // Can be price, description, etc.
    val imageResId: Int = R.drawable.ic_placeholder_image
)