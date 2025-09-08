package com.rst.gadissalonmanagementsystemapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    // Holds the product currently being viewed in a detail screen
    private val _currentlyViewedProduct = MutableLiveData<Product?>()

    // LiveData to observe the favorite status of the current product
    private val _isCurrentProductFavorite = MutableLiveData<Boolean>()
    val isCurrentProductFavorite: LiveData<Boolean> = _isCurrentProductFavorite

    fun setCurrentProduct(product: Product) {
        _currentlyViewedProduct.value = product
        _isCurrentProductFavorite.value = AppData.isFavorite(product)
    }

    fun onFavoriteClicked() {
        _currentlyViewedProduct.value?.let { product ->
            AppData.toggleFavorite(product)
            // Update the LiveData to notify observers (the Activity)
            _isCurrentProductFavorite.value = AppData.isFavorite(product)
        }
    }
}