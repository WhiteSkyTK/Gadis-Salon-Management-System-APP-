package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.firestore.Exclude

data class FaqItem(
    var id: String = "",
    val question: String = "",
    val answer: String = "",
    @get:Exclude var isExpanded: Boolean = false
)