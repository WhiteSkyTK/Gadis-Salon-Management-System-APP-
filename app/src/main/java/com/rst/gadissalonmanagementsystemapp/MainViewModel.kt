package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Holds the product currently being viewed in a detail screen
    private val _currentlyViewedProduct = MutableLiveData<Product?>()

    // LiveData to observe the favorite status of the current product
    private val _isCurrentProductFavorite = MutableLiveData<Boolean>()
    val isCurrentProductFavorite: LiveData<Boolean> = _isCurrentProductFavorite

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    fun setCurrentProduct(product: Product) {
        _currentlyViewedProduct.value = product
        // Asynchronously check if the product is a favorite
        viewModelScope.launch {
            val result = FirebaseManager.isFavorite(product.id)
            _isCurrentProductFavorite.value = result.getOrNull() == true
        }
    }

    fun onFavoriteClicked() {
        _currentlyViewedProduct.value?.let { product ->
            viewModelScope.launch {
                val result = FirebaseManager.toggleFavorite(product)
                if (result.isSuccess) {
                    _isCurrentProductFavorite.value = result.getOrDefault(false)
                }
            }
        }
    }

    fun loadCurrentUser() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                val result = FirebaseManager.getUser(uid)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                } else {
                    _currentUser.value = null
                }
            }
        }
    }
}