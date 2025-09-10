package com.rst.gadissalonmanagementsystemapp

import java.util.UUID // Import UUID

data class User(
    val id: String = UUID.randomUUID().toString(), // Add a unique ID
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    var imageUrl: String = "",
    val role: String = "CUSTOMER",
    val favorites: MutableList<Favoritable> = mutableListOf(),
    val cart: MutableList<CartItem> = mutableListOf()
)