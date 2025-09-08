package com.rst.gadissalonmanagementsystemapp

data class FaqItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false // To track if the answer is visible
)