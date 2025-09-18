package com.rst.gadissalonmanagementsystemapp

data class CartItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var quantity: Int = 0,
    val imageUrl: String = ""
)