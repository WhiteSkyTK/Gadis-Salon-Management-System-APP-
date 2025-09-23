package com.rst.gadissalonmanagementsystemapp.ui.favorites

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.Favoritable
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemFavoriteBinding
import java.text.NumberFormat
import java.util.Locale

class FavoritesAdapter(
    private var items: List<Favoritable>,
    private val onUnfavoriteClick: (Favoritable) -> Unit,
    private val onAddToCartClick: (Product) -> Unit,
    private val onBookClick: (Hairstyle) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TAG = "FavoritesAdapter"

    companion object {
        private const val VIEW_TYPE_PRODUCT = 1
        private const val VIEW_TYPE_HAIRSTYLE = 2
    }

    // --- ViewHolder for Products ---
    inner class ProductViewHolder(private val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Product) {
            Log.d(TAG, "Binding Product: ${item.name}")
            Log.d(TAG, "Product Variants: ${item.variants}")
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.itemName.text = item.name
            val price = item.variants.firstOrNull()?.price ?: 0.0
            Log.d(TAG, "Extracted price for ${item.name}: R$price") // See the exact price
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(price)

            binding.itemSize.visibility = View.VISIBLE
            binding.itemSize.text = item.variants.firstOrNull()?.size

            binding.addToCartButton.visibility = View.VISIBLE
            binding.bookNowButton.visibility = View.GONE

            binding.addToCartButton.setOnClickListener { onAddToCartClick(item) }
            binding.unfavoriteButton.setOnClickListener { onUnfavoriteClick(item) }
        }
    }

    // --- ViewHolder for Hairstyles ---
    inner class HairstyleViewHolder(private val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Hairstyle) {
            Log.d(TAG, "Binding Hairstyle: ${item.name} with price R${item.price}")

            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.itemName.text = item.name
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(item.price)
            binding.itemSize.visibility = View.GONE

            binding.addToCartButton.visibility = View.GONE
            binding.bookNowButton.visibility = View.VISIBLE

            binding.unfavoriteButton.setOnClickListener { onUnfavoriteClick(item) }
            binding.bookNowButton.setOnClickListener {
                val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookingConfirmationFragment(item)
                itemView.findNavController().navigate(action)
            }
        }
    }

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

    fun updateData(newItems: List<Favoritable>) {
        this.items = newItems
        notifyDataSetChanged() // Tells the RecyclerView to redraw itself
    }
}