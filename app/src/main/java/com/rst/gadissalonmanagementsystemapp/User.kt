package com.rst.gadissalonmanagementsystemapp

data class User(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "CUSTOMER",
    val favorites: MutableList<Product> = mutableListOf(),
    val cart: MutableList<CartItem> = mutableListOf()
)