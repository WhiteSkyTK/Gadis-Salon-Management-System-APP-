package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCartBinding

class OrderDetailAdapter(private val items: List<CartItem>) : RecyclerView.Adapter<OrderDetailAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.name
            binding.itemSize.text = item.size
            binding.itemQuantity.text = "Qty: ${item.quantity}"
            binding.itemImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_placeholder_image)
            }

            // Hide the interactive elements as this is just a summary view
            binding.buttonPlus.visibility = View.GONE
            binding.buttonMinus.visibility = View.GONE
            binding.buttonRemove.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
