package com.rst.gadissalonmanagementsystemapp.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemSize.text = item.size
            binding.itemQuantity.text = item.quantity.toString()
            binding.itemImage.load(item.imageUrl) { placeholder(R.drawable.ic_placeholder_image) }
            binding.itemPrice.text = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(item.price)

            // --- Click Listeners for Interactive Buttons ---
            binding.buttonPlus.setOnClickListener {
                onQuantityChanged(item, item.quantity + 1)
            }
            binding.buttonMinus.setOnClickListener {
                onQuantityChanged(item, item.quantity - 1)
            }
            binding.buttonRemove.setOnClickListener {
                onRemove(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateData(newCartItems: List<CartItem>) {
        this.cartItems = newCartItems
        notifyDataSetChanged()
    }
}

