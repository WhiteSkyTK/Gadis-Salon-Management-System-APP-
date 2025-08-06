package com.rst.gadissalonmanagementsystemapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.databinding.ItemFavoriteBinding

class FavoritesAdapter(
    private val items: List<Product>,
    private val onUnfavoriteClick: (Product) -> Unit,
    private val onAddToCartClick: (Product) -> Unit,
    private val onBookClick: (Product) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(private val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Product) {
            binding.itemImage.setImageResource(item.imageResId)
            binding.itemName.text = item.name
            binding.itemPrice.text = item.detail
            binding.itemSize.visibility = View.GONE

            when (item.type) {
                Product.TYPE_PRODUCT -> {
                    binding.addToCartButton.visibility = View.VISIBLE
                    binding.bookNowButton.visibility = View.GONE
                    binding.addToCartButton.setOnClickListener { onAddToCartClick(item) }
                }
                Product.TYPE_HAIRSTYLE -> {
                    binding.addToCartButton.visibility = View.GONE
                    binding.bookNowButton.visibility = View.VISIBLE
                    binding.bookNowButton.setOnClickListener { onBookClick(item) }
                }
            }

            binding.unfavoriteButton.setOnClickListener { onUnfavoriteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}