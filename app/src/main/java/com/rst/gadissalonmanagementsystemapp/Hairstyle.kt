package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hairstyle(
    override val id: String = "",
    override val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val durationHours: Int = 0,
    val availableStylistIds: List<String> = emptyList(),
    val imageUrl: String = ""
) : Parcelable, Favoritable