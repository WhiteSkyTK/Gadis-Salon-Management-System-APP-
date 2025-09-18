package com.rst.gadissalonmanagementsystemapp.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForFavoriteUpdates()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            items = emptyList(),
            onUnfavoriteClick = { item ->
                // When an item is unfavorited, call the appropriate FirebaseManager function
                viewLifecycleOwner.lifecycleScope.launch {
                    when (item) {
                        is Product -> FirebaseManager.toggleFavorite(item)
                        is Hairstyle -> FirebaseManager.toggleFavorite(item)
                    }
                }
            },
            onAddToCartClick = { product ->
                val variant = product.variants.firstOrNull()
                if (variant != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseManager.addToCart(product, variant)
                        Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBookClick = { hairstyle ->
                val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookingConfirmationFragment(hairstyle)
                findNavController().navigate(action)
            }
        )
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.favoritesRecyclerView.adapter = favoritesAdapter
    }

    private fun listenForFavoriteUpdates() {
        // Start listening for real-time updates to the current user's favorites
        FirebaseManager.addCurrentUserFavoritesListener { favoritesList ->
            // When the data changes, update the adapter's list
            favoritesAdapter.updateData(favoritesList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}