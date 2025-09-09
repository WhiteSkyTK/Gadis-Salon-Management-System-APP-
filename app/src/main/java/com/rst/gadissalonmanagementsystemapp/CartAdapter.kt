package com.rst.gadissalonmanagementsystemapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(private val items: List<CartItem>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemQuantity.text = item.quantity.toString()
            binding.itemImage.setImageResource(item.imageResId)

            // Format the price to show currency
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