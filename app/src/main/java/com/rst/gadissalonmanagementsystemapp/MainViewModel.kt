package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Holds the product currently being viewed in a detail screen
    private val _currentlyViewedProduct = MutableLiveData<Product?>()

    // LiveData to observe the favorite status of the current product
    private val _isCurrentProductFavorite = MutableLiveData<Boolean>()
    val isCurrentProductFavorite: LiveData<Boolean> = _isCurrentProductFavorite

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
}