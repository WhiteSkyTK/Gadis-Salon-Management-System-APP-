package com.rst.gadissalonmanagementsystemapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.ItemFavoriteBinding
import com.rst.gadissalonmanagementsystemapp.ui.favorites.FavoritesFragmentDirections
import java.text.NumberFormat
import java.util.Locale

class FavoritesAdapter(
    private val items: List<Favoritable>,
    private val onUnfavoriteClick: (Favoritable) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define constants for our view types
    companion object {
        private const val VIEW_TYPE_PRODUCT = 1
        private const val VIEW_TYPE_HAIRSTYLE = 2
    }

    // --- ViewHolder for Products ---
    inner class ProductViewHolder(private val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Product) {
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.itemName.text = item.name
            val price = item.variants.firstOrNull()?.price ?: 0.0
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(price)
            binding.itemSize.visibility = View.VISIBLE
            binding.itemSize.text = item.variants.firstOrNull()?.size

            // Show product-specific buttons
            binding.addToCartButton.visibility = View.VISIBLE
            binding.bookNowButton.visibility = View.GONE

            binding.unfavoriteButton.setOnClickListener { onUnfavoriteClick(item) }
            binding.addToCartButton.setOnClickListener {
                AppData.addToCart(item)
                Toast.makeText(itemView.context, "${item.name} added to cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- ViewHolder for Hairstyles ---
    inner class HairstyleViewHolder(private val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Hairstyle) {
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.itemName.text = item.name
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(item.price)
            binding.itemSize.visibility = View.GONE

            // Show hairstyle-specific buttons
            binding.addToCartButton.visibility = View.GONE
            binding.bookNowButton.visibility = View.VISIBLE

            binding.unfavoriteButton.setOnClickListener { onUnfavoriteClick(item) }
            binding.bookNowButton.setOnClickListener {
                val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookingConfirmationFragment(item)
                itemView.findNavController().navigate(action)
            }
        }
    }

    // This method tells the adapter which layout to use for which item
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Product -> VIEW_TYPE_PRODUCT
            is Hairstyle -> VIEW_TYPE_HAIRSTYLE
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return when (viewType) {
            VIEW_TYPE_PRODUCT -> ProductViewHolder(binding)
            VIEW_TYPE_HAIRSTYLE -> HairstyleViewHolder(binding)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ProductViewHolder -> holder.bind(items[position] as Product)
            is HairstyleViewHolder -> holder.bind(items[position] as Hairstyle)
        }
    }

    override fun getItemCount(): Int = items.size
}