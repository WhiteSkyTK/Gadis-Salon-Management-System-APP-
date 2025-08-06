package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// A Singleton object to hold our app's temporary data
object AppData {

    // --- MASTER DATA ---
    val allProducts = listOf(
        Product(
            id = "prod_01",
            name = "Eco Style Gel",
            reviews = "152 Reviews",
            variants = listOf(
                ProductVariant("250ml", 50.00),
                ProductVariant("500ml", 85.00)
            )
        ),
        Product(
            id = "prod_02",
            name = "Hair Spray",
            reviews = "98 Reviews",
            variants = listOf(
                ProductVariant("150ml", 120.00, 140.00), // Example with a discount
                ProductVariant("300ml", 200.00)
            )
        ),
        Product(
            id = "prod_03",
            name = "Shampoo",
            reviews = "210 Reviews",
            variants = listOf(
                ProductVariant("400ml", 80.00) // Product with only one size
            )
        )
    )

    // --- NEW MASTER DATA for STYLISTS and HAIRSTYLES ---
    val allStylists = listOf(
        Stylist("stylist_01", "Sarah"),
        Stylist("stylist_02", "Rinae"), // Using your team member's name
        Stylist("stylist_03", "Jane")
    )

    val allHairstyles = listOf(
        Hairstyle("hs_01", "Butterfly Locs", "Elegant and protective", 450.0, 4, listOf("stylist_01", "stylist_02")),
        Hairstyle("hs_02", "Dreadlocks", "Classic dreadlocks retwist", 400.0, 3, listOf("stylist_01", "stylist_03")),
        Hairstyle("hs_03", "Cornrows", "Neat and stylish cornrows", 250.0, 2, listOf("stylist_02")),
        Hairstyle("hs_04", "Box Braids", "Long-lasting box braids", 500.0, 5, listOf("stylist_01", "stylist_02", "stylist_03"))
    )

    val availableTimeSlots = listOf("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")

    // --- USER MANAGEMENT ---
    private val registeredUsers = mutableListOf<User>()
    private var currentUser: User? = null

    fun getCurrentUser(): User? {
        return currentUser
    }

    // --- New LiveData that reflects the CURRENT user's data ---
    private val _currentUserFavorites = MutableLiveData<List<Favoritable>>(emptyList())
    val currentUserFavorites: LiveData<List<Favoritable>> = _currentUserFavorites

    private val _currentUserCart = MutableLiveData<List<CartItem>>()
    val currentUserCart: LiveData<List<CartItem>> = _currentUserCart

    fun registerUser(name: String, email: String, phone: String, password: String): Boolean {
        // Check if email is already taken
        if (registeredUsers.any { it.email.equals(email, ignoreCase = true) }) {
            return false // Registration failed
        }
        // Add new user
        val newUser = User(name, email, phone, password)
        registeredUsers.add(newUser)
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
                user.cart.add(CartItem(product.name, price, 1, product.imageResId))
            }
            _currentUserCart.value = user.cart // Update LiveData
        }
    }

    fun logoutUser() {
        currentUser = null
        _currentUserFavorites.value = emptyList()
        _currentUserCart.value = emptyList()
    }

    fun loginUser(email: String, password: String): String? {
        val foundUser = registeredUsers.find { it.email.equals(email, ignoreCase = true) && it.password == password }
        return if (foundUser != null) {
            currentUser = foundUser
            // Notify LiveData observers
            _currentUserFavorites.value = currentUser?.favorites
            _currentUserCart.value = currentUser?.cart
            currentUser?.role // Login successful, return role
        } else {
            null // Login failed
        }
    }
    // Add functions to remove from cart, change quantity etc. here
}