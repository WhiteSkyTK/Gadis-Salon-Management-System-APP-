package com.rst.gadissalonmanagementsystemapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale
import coil.load

class CartAdapter(private val items: List<CartItem>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemQuantity.text = item.quantity.toString()

            // Use Coil to load the image from the URL
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image) // Show placeholder while loading
                error(R.drawable.ic_placeholder_image) // Show placeholder if image fails to load
            }

            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(item.price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}