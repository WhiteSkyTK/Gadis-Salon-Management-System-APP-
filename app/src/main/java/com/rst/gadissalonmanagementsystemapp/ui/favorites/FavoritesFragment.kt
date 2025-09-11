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
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.FavoritesAdapter
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(context)

        AppData.currentUserFavorites.observe(viewLifecycleOwner) { favoritesList ->
            val adapter = FavoritesAdapter(
                items = favoritesList,
                onUnfavoriteClick = { item ->
                    when(item) {
                        is Product -> AppData.toggleFavorite(item)
                        is Hairstyle -> AppData.toggleFavorite(item)
                    }
                },
                onAddToCartClick = { product ->
                    // Find the default variant to add to the cart
                    val variant = product.variants.firstOrNull()
                    if (variant != null) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseManager.addToCart(product, variant)
                            Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "This product has no sizes available.", Toast.LENGTH_SHORT).show()
                    }
                },
                onBookClick = { hairstyle ->
                    // We need to use the action that goes to the Booking CONFIRMATION fragment
                    val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookingConfirmationFragment(hairstyle)
                    findNavController().navigate(action)
                }
            )
            binding.favoritesRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}