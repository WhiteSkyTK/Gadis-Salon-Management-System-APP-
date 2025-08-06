package com.rst.gadissalonmanagementsystemapp

data class CartItem(
    val name: String,
    val price: Double,
    var quantity: Int, // 'var' so we can change it
    val imageResId: Int = R.drawable.ic_placeholder_image
)