package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    // The ID should be a 'var' with a default empty string.
    // Firestore will populate this with the real Document ID.
    var id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    var imageUrl: String = "",
    val role: String = "CUSTOMER"
    // We don't need to parcelize favorites or cart
) : Parcelable