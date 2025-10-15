package com.rst.gadissalonmanagementsystemapp.ui.favorites

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFavoritesBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesAdapter: FavoritesAdapter
    private var favoritesListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForFavoriteUpdates()
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForFavoriteUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        favoritesListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
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
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.favoritesRecyclerView.visibility = View.GONE
        binding.emptyFavoritesText.visibility = View.GONE

        favoritesListener = FirebaseManager.addCurrentUserFavoritesListener { favoritesList ->
            if (view != null) {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE

                Log.d("FavoritesFragment", "Listener callback received with ${favoritesList.size} items.")
                if (favoritesList.isEmpty()) {
                    binding.favoritesRecyclerView.visibility = View.GONE
                    binding.emptyFavoritesText.visibility = View.VISIBLE
                } else {
                    binding.favoritesRecyclerView.visibility = View.VISIBLE
                    binding.emptyFavoritesText.visibility = View.GONE
                    favoritesAdapter.updateData(favoritesList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}