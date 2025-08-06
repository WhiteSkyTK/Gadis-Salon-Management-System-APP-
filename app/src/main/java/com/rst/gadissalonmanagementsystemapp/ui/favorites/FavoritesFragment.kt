package com.rst.gadissalonmanagementsystemapp.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.FavoritesAdapter
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the layout manager for the RecyclerView
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Observe the LiveData from AppData
        AppData.favoriteItems.observe(viewLifecycleOwner) { favoritesList ->
            val adapter = FavoritesAdapter(
                items = favoritesList,
                onUnfavoriteClick = { product ->
                    AppData.toggleFavorite(product)
                },
                onAddToCartClick = { product ->
                    AppData.addToCart(product)
                    Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                },
                // Add the new click logic for booking
                onBookClick = { hairstyle ->
                    // For now, let's just show a message and navigate to the main booking page
                    Toast.makeText(context, "Booking for ${hairstyle.name}", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.bookingFragment)
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