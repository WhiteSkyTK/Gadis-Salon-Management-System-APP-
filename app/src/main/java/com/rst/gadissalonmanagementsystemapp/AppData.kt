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

    fun addProduct(product: Product) {
        val currentList = _allProducts.value?.toMutableList() ?: mutableListOf()
        currentList.add(product)
        _allProducts.value = currentList
    }

    fun deleteProduct(productId: String) {
        val currentList = _allProducts.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.id == productId }
        _allProducts.value = currentList
    }

    fun updateProduct(updatedProduct: Product) {
        val currentList = _allProducts.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            currentList[index] = updatedProduct
            _allProducts.value = currentList
        }
    }

    fun addHairstyle(hairstyle: Hairstyle) {
        val currentList = _allHairstyles.value?.toMutableList() ?: mutableListOf()
        currentList.add(hairstyle)
        _allHairstyles.value = currentList
    }

    fun deleteHairstyle(hairstyleId: String) {
        val currentList = _allHairstyles.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.id == hairstyleId }
        _allHairstyles.value = currentList
    }

    fun updateHairstyle(updatedHairstyle: Hairstyle) {
        val currentList = _allHairstyles.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == updatedHairstyle.id }
        if (index != -1) {
            currentList[index] = updatedHairstyle
            _allHairstyles.value = currentList
        }
    }

    // --- NEW WORKER/ADMIN FUNCTIONS for booking management ---

    fun updateBookingStatus(bookingId: String, newStatus: String, stylist: User?) {
        val currentList = _allBookings.value?.toMutableList() ?: return
        val bookingIndex = currentList.indexOfFirst { it.id == bookingId }

        if (bookingIndex != -1) {
            val oldBooking = currentList[bookingIndex]
            // Update the booking with the new status and the stylist who accepted it
            currentList[bookingIndex] = oldBooking.copy(
                status = newStatus,
                stylistName = stylist?.name ?: oldBooking.stylistName
            )
            _allBookings.value = currentList
        }
    }

    // --- USER MANAGEMENT ---
    private val _registeredUsers = MutableLiveData<List<User>>(emptyList()) // Start with an empty list
    val registeredUsers: LiveData<List<User>> = _registeredUsers
    private var currentUser: User? = null


    fun addUser(user: User) {
        val currentList = _registeredUsers.value?.toMutableList() ?: mutableListOf()
        currentList.add(user)
        _registeredUsers.value = currentList
    }

    fun removeUser(userId: String) {
        val currentList = _registeredUsers.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.id == userId }
        _registeredUsers.value = currentList
    }

    fun getRegisteredUsers(): List<User> {
        return _registeredUsers.value?.toList() ?: emptyList()
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    // --- New LiveData that reflects the CURRENT user's data ---
    private val _currentUserFavorites = MutableLiveData<List<Favoritable>>(emptyList())
    val currentUserFavorites: LiveData<List<Favoritable>> = _currentUserFavorites

    private val _currentUserCart = MutableLiveData<List<CartItem>>()
    val currentUserCart: LiveData<List<CartItem>> = _currentUserCart

    fun registerUser(name: String, email: String, phone: String, password: String): Boolean {
        val currentList = _registeredUsers.value ?: emptyList()
        // Check if email is already taken
        if (currentList.any { it.email.equals(email, ignoreCase = true) }) {
            return false // Registration failed
        }
        // Add new user to a new list and update the LiveData
        val newList = currentList.toMutableList()
        newList.add(User(name = name, email = email, phone = phone))
        _registeredUsers.value = newList
        return true // Registration successful
    }

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

    fun isFavorite(item: Favoritable): Boolean {
        return _currentUserFavorites.value?.any { it.id == item.id } ?: false
    }

    fun addToCart(product: Product) {
        currentUser?.let { user ->
            val existingItem = user.cart.find { it.name == product.name }

            if (existingItem != null) {
                existingItem.quantity++
            } else {
                // FIX: Get the price from the first variant in the list
                val price = product.variants.firstOrNull()?.price ?: 0.0
                user.cart.add(CartItem(product.name, price, 1, product.imageUrl))
            }
            _currentUserCart.value = user.cart // Update LiveData
        }
    }

    fun logoutUser() {
        currentUser = null
        _currentUserFavorites.value = emptyList()
        _currentUserCart.value = emptyList()
    }

    fun loginUser(email: String): String? {
        val foundUser = _registeredUsers.value?.find { it.email.equals(email, ignoreCase = true)}
        return if (foundUser != null) {
            currentUser = foundUser
            _currentUserFavorites.value = currentUser?.favorites
            _currentUserCart.value = currentUser?.cart
            currentUser?.role
        } else {
            null
        }
    }
    // Add functions to remove from cart, change quantity etc. here
}