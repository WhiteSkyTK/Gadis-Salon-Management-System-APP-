package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hairstyle(
    override val id: String, // Add 'override'
    override val name: String, // Add 'override'
    val description: String,
    val price: Double,
    val durationHours: Int,
    val availableStylistIds: List<String>,
    val imageResId: Int = R.drawable.ic_placeholder_image
) : Parcelable, Favoritable // Implement Favoritable