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
    private val _currentlyViewedItem = MutableLiveData<Favoritable?>()

    private val _isCurrentItemFavorite = MutableLiveData<Boolean>()
    val isCurrentItemFavorite: LiveData<Boolean> = _isCurrentItemFavorite

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _allHairstyles = MutableLiveData<List<Hairstyle>>()
    val allHairstyles: LiveData<List<Hairstyle>> = _allHairstyles

    fun setCurrentFavoritableItem(item: Favoritable) {
        _currentlyViewedItem.value = item
        viewModelScope.launch {
            val result = FirebaseManager.isFavorite(item.id)
            _isCurrentItemFavorite.value = result.getOrNull() == true
        }
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
                // Check the type and call the correct function
                val result = when (item) {
                    is Product -> FirebaseManager.toggleFavorite(item)
                    is Hairstyle -> FirebaseManager.toggleFavorite(item)
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