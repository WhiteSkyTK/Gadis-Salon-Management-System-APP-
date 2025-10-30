package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    override var id: String = "",
    override val name: String = "",
    override val type: String = "PRODUCT",
    val reviews: String = "",
    val variants: List<ProductVariant> = emptyList(),
    val imageUrl: String = ""
) : Parcelable, Favoritable