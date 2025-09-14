package com.rst.gadissalonmanagementsystemapp

// Represents a customer's completed purchase of products
data class ProductOrder(
    val id: String = "",
    val customerName: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "Pending Pickup" // e.g., "Pending Pickup", "Completed"
)