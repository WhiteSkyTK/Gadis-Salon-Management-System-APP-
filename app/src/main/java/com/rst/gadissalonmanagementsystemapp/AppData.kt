package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object AppData {

    // --- MASTER DATA ---
    // These are the single source of truth for your products and hairstyles.
    private val _allProducts = MutableLiveData<List<Product>>(
        listOf(
            Product(
                id = "prod_01", name = "Eco Style Gel",
                variants = listOf(ProductVariant("250ml", 50.00, stock = 25))
            ),
            Product(
                id = "prod_02", name = "Hair Spray",
                variants = listOf(ProductVariant("150ml", 120.00, 140.00, stock = 15))
            ),
            Product(
                id = "prod_03", name = "Shampoo",
                variants = listOf(ProductVariant("400ml", 80.00, stock = 50))
            )
        )
    )
    val allProducts: LiveData<List<Product>> = _allProducts

    private val _allHairstyles = MutableLiveData<List<Hairstyle>>(
        listOf(
            Hairstyle("hs_01", "Butterfly Locs", "Elegant and protective", 450.0, 4, listOf("stylist_01", "stylist_02")),
            Hairstyle("hs_02", "Dreadlocks", "Classic dreadlocks retwist", 400.0, 3, listOf("stylist_01", "stylist_03")),
            Hairstyle("hs_03", "Cornrows", "Neat and stylish cornrows", 250.0, 2, listOf("stylist_02")),
            Hairstyle("hs_04", "Box Braids", "Long-lasting box braids", 500.0, 5, listOf("stylist_01", "stylist_02", "stylist_03"))
        )
    )
    val allHairstyles: LiveData<List<Hairstyle>> = _allHairstyles

    val availableTimeSlots = listOf("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")


    // --- ADMIN MANAGEMENT FUNCTIONS ---
    // --- NEW MASTER LIST for BOOKINGS ---
    private val _allBookings = MutableLiveData<List<AdminBooking>>(emptyList())
    val allBookings: LiveData<List<AdminBooking>> = _allBookings

    // --- NEW FUNCTION for customers to create bookings ---
    fun addBooking(booking: AdminBooking) {
        val currentList = _allBookings.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, booking) // Add new bookings to the top of the list
        _allBookings.value = currentList
    }

    // --- NEW WORKER/ADMIN FUNCTIONS for booking management ---


    private var currentUser: User? = null


    fun getCurrentUser(): User? {
        return currentUser
    }

    // --- New LiveData that reflects the CURRENT user's data ---
    private val _currentUserFavorites = MutableLiveData<List<Favoritable>>(emptyList())
    val currentUserFavorites: LiveData<List<Favoritable>> = _currentUserFavorites

    private val _currentUserCart = MutableLiveData<List<CartItem>>()
    val currentUserCart: LiveData<List<CartItem>> = _currentUserCart

    // Now we can have a function for each type
    fun toggleFavorite(product: Product) {
        val currentFavorites = _currentUserFavorites.value?.toMutableList() ?: mutableListOf()
        if (currentFavorites.any { it is Product && it.id == product.id }) {
            currentFavorites.removeAll { it is Product && it.id == product.id }
        } else {
            currentFavorites.add(product)
        }
        _currentUserFavorites.value = currentFavorites
    }

    fun toggleFavorite(hairstyle: Hairstyle) {
        val currentFavorites = _currentUserFavorites.value?.toMutableList() ?: mutableListOf()
        if (currentFavorites.any { it is Hairstyle && it.id == hairstyle.id }) {
            currentFavorites.removeAll { it is Hairstyle && it.id == hairstyle.id }
        } else {
            currentFavorites.add(hairstyle)
        }
        _currentUserFavorites.value = currentFavorites
    }

}
