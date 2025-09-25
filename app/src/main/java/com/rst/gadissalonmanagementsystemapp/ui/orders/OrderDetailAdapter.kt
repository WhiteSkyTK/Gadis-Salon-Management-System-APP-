package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemOrderSummaryBinding
import java.text.NumberFormat
import java.util.Locale

class OrderDetailAdapter(private val items: List<CartItem>) : RecyclerView.Adapter<OrderDetailAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemOrderSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemSize.text = item.size
            binding.itemQuantity.text = "Qty: ${item.quantity}"
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemPrice.text = format.format(item.price * item.quantity)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
