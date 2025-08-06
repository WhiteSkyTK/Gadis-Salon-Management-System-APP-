package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// A Singleton object to hold our app's temporary data
object AppData {

    // --- MASTER DATA ---
    val allProducts = listOf(
        Product("Eco Style Gel", "R50", Product.TYPE_PRODUCT),
        Product("Shampoo", "R80", Product.TYPE_PRODUCT),
        Product("Conditioner", "R85", Product.TYPE_PRODUCT),
        Product("Hair Spray", "R120", Product.TYPE_PRODUCT),
        Product("Leave-in Treatment", "R150", Product.TYPE_PRODUCT),
        Product("Hair Food", "R65", Product.TYPE_PRODUCT)
    )

    val allHairstyles = listOf(
        Product("Butterfly Locs", "R450", Product.TYPE_HAIRSTYLE),
        Product("Dreadlocks", "R400", Product.TYPE_HAIRSTYLE),
        Product("Cornrows", "R250", Product.TYPE_HAIRSTYLE),
        Product("Box Braids", "R500", Product.TYPE_HAIRSTYLE),
        Product("Faux Locs", "R600", Product.TYPE_HAIRSTYLE),
        Product("Twists", "R350", Product.TYPE_HAIRSTYLE)
    )

    // --- USER MANAGEMENT ---
    private val registeredUsers = mutableListOf<User>()
    private var currentUser: User? = null

    fun getCurrentUser(): User? {
        return currentUser
    }

    // --- New LiveData that reflects the CURRENT user's data ---
    private val _currentUserFavorites = MutableLiveData<List<Product>>()
    val currentUserFavorites: LiveData<List<Product>> = _currentUserFavorites

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

    fun toggleFavorite(product: Product) {
        currentUser?.let { user ->
            if (user.favorites.contains(product)) {
                user.favorites.remove(product)
            } else {
                user.favorites.add(product)
            }
            _currentUserFavorites.value = user.favorites // Update LiveData
        }
    }

    fun isFavorite(product: Product): Boolean {
        return currentUser?.favorites?.contains(product) ?: false
    }

    fun addToCart(product: Product) {
        currentUser?.let { user ->
            val existingItem = user.cart.find { it.name == product.name }
            if (existingItem != null) {
                existingItem.quantity++
            } else {
                val price = product.detail.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
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