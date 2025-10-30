package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import kotlinx.parcelize.Parcelize

// This data class holds a product variant that has been favorited
@Parcelize
data class FavoriteItem(
    override var id: String = "",
    override val name: String = "",
    override val type: String = "PRODUCT",
    val originalId: String = "", // The ID of the main product (e.g., "prod_..._")
    val imageUrl: String = "",
    val isVariantFavorite: Boolean = true,
    val favoritedVariant: ProductVariant = ProductVariant()
) : Parcelable, Favoritable

