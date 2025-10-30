package com.rst.gadissalonmanagementsystemapp

import android.os.Parcelable

// This interface now just marks items that can be in the favorites list
interface Favoritable : Parcelable {
    var id: String // All favoritable items MUST have an ID
    val name: String
    val type: String
}
