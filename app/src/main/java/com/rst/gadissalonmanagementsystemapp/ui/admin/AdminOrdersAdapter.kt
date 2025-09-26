package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.ProductOrder
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminOrdersAdapter(private var orders: List<ProductOrder>) : RecyclerView.Adapter<AdminOrdersAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: ProductOrder) {
            binding.customerNameValue.text = order.customerName
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            binding.orderDateValue.text = sdf.format(Date(order.timestamp))
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.totalPriceValue.text = "Total: ${format.format(order.totalPrice)}"
            binding.orderStatusAdmin.text = order.status

            // --- Logic to display up to 3 product images ---
            val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3)
            // Hide all images initially to handle orders with fewer than 3 items
            imageViews.forEach { it.visibility = View.GONE }

            // Show and load images for the first few items in the order
            order.items.take(3).forEachIndexed { index, cartItem ->
                imageViews[index].visibility = View.VISIBLE
                imageViews[index].load(cartItem.imageUrl) {
                    placeholder(R.drawable.ic_placeholder_image)
                    error(R.drawable.ic_placeholder_image)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateData(newOrders: List<ProductOrder>) {
        this.orders = newOrders
        notifyDataSetChanged()
    }
}
