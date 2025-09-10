package com.rst.gadissalonmanagementsystemapp

// Data class for storing salon location details in Firestore
data class SalonLocation(
    val addressLine1: String = "Makatu St",
    val addressLine2: String = "House no 995",
    val latitude: Double = -23.220938,
    val longitude: Double = 29.993942
)