package com.rst.gadissalonmanagementsystemapp.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit, // This will now update locally
    private val onRemove: (CartItem) -> Unit, // This will now update locally
    private val onStockError: (String) -> Unit // Callback for showing stock errors
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemSize.text = item.size
            binding.itemQuantity.text = item.quantity.toString()
            binding.itemImage.load(item.imageUrl) { placeholder(R.drawable.ic_placeholder_image) }
            binding.itemPrice.text = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(item.price)

            // --- NEW: Sold Out and Stock Limit Logic ---
            if (item.stock <= 0) {
                // Sold out
                binding.itemSoldOutText.visibility = View.VISIBLE
                binding.quantityControls.visibility = View.GONE
                binding.itemQuantity.text = "0"
                item.quantity = 0 // Ensure quantity is 0
            } else {
                // In stock
                binding.itemSoldOutText.visibility = View.GONE
                binding.quantityControls.visibility = View.VISIBLE

                // Disable plus button if quantity equals stock
                binding.buttonPlus.isEnabled = item.quantity < item.stock
                // Disable minus button if quantity is 1
                binding.buttonMinus.isEnabled = item.quantity > 1
            }
            // --- END NEW ---

            binding.buttonPlus.setOnClickListener {
                if (item.quantity < item.stock) {
                    onQuantityChanged(item, item.quantity + 1)
                } else {
                    onStockError("No more stock available for ${item.name}.")
                }
            }
            binding.buttonMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChanged(item, item.quantity - 1)
                }
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
        cartItems.clear()
        cartItems.addAll(newCartItems)
        notifyDataSetChanged()
    }
}

