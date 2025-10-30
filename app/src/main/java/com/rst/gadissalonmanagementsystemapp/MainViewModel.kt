package com.rst.gadissalonmanagementsystemapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Holds the product currently being viewed in a detail screen
    private val _currentlyViewedItem = MutableLiveData<Favoritable?>()
    val currentlyViewedItem: LiveData<Favoritable?> = _currentlyViewedItem // --- Expose this ---

    private val _currentlySelectedVariant = MutableLiveData<ProductVariant?>()

    private val _isCurrentItemFavorite = MutableLiveData<Boolean>()
    val isCurrentItemFavorite: LiveData<Boolean> = _isCurrentItemFavorite

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _allHairstyles = MutableLiveData<List<Hairstyle>>()
    val allHairstyles: LiveData<List<Hairstyle>> = _allHairstyles

    fun setCurrentFavoritableItem(item: Favoritable) {
        _currentlyViewedItem.value = item
        // --- MODIFIED: Clear the variant when a new item is set ---
        _currentlySelectedVariant.value = null

        // If it's a hairstyle, we can check favorite status immediately
        if (item is Hairstyle) {
            viewModelScope.launch {
                val result = FirebaseManager.isFavorite(item.id)
                _isCurrentItemFavorite.value = result.getOrNull() == true
            }
        }
        // If it's a product, the ProductDetailFragment will call
        // checkIfFavorite() once a variant is selected.
    }

    fun setFavoriteState(isFavorite: Boolean) {
        _isCurrentItemFavorite.value = isFavorite
    }

    fun setSelectedVariant(variant: ProductVariant?) {
        _currentlySelectedVariant.value = variant
    }

    fun loadAllHairstyles() {
        viewModelScope.launch {
            val result = FirebaseManager.getAllHairstyles()
            if (result.isSuccess) {
                _allHairstyles.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun onFavoriteClicked() {
        _currentlyViewedItem.value?.let { item ->
            viewModelScope.launch {
                val result: Result<Boolean>

                when (item) {
                    is Hairstyle -> {
                        // Hairstyle logic is simple
                        result = FirebaseManager.toggleFavorite(item)
                    }
                    is Product -> {
                        // Product logic requires a variant
                        val product = item
                        val variant = _currentlySelectedVariant.value

                        if (variant == null) {
                            Log.w("MainViewModel", "Favorite clicked but no variant is selected.")
                            // Optionally, you could send a Toast message here via a LiveData event
                            return@launch // Do nothing
                        }

                        Log.d("MainViewModel", "Toggling favorite for ${product.name} - ${variant.size}")
                        result = FirebaseManager.toggleFavoriteVariant(product, variant)
                    }
                    is FavoriteItem -> {
                        // This case handles if the user is viewing a favorite
                        // from the favorites list, which is already a FavoriteItem
                        val variant = item.favoritedVariant
                        val product = Product(id = item.originalId, name = item.name, imageUrl = item.imageUrl, type = item.type)

                        Log.d("MainViewModel", "Toggling favorite for ${product.name} - ${variant.size}")
                        result = FirebaseManager.toggleFavoriteVariant(product, variant)
                    }
                    else -> return@launch // Should not happen
                }

                if (result.isSuccess) {
                    _isCurrentItemFavorite.value = result.getOrDefault(false)
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