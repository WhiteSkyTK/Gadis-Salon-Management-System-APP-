package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hairstyle(
    override var id: String = "",
    override val name: String = "",
    override val type: String = "HAIRSTYLE",
    val description: String = "",
    val price: Double = 0.0,
    val durationHours: Int = 0,
    val availableStylistIds: List<String> = emptyList(),
    val imageUrl: String = ""
) : Parcelable, Favoritable