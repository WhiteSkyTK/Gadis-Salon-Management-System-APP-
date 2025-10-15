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
import java.util.*

class AdminOrderAdapter(
    private var orders: List<ProductOrder>,
    private val onItemClick: (ProductOrder) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: ProductOrder) {
            binding.customerNameValue.text = order.customerName
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            binding.orderDateValue.text = order.timestamp?.let { sdf.format(Date(it)) } ?: "No date"

            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.totalPriceValue.text = "Total: ${format.format(order.totalPrice)}"

            binding.orderStatusAdmin.text = order.status
            val statusColor = when (order.status.lowercase()) {
                "completed" -> R.color.status_green
                "cancelled", "abandoned" -> R.color.status_red
                "ready for pickup" -> R.color.colorPrimary2
                else -> R.color.status_grey // Pending Pickup
            }
            binding.orderStatusAdmin.setChipBackgroundColorResource(statusColor)

            // Handle stacked images
            val images = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3)
            images.forEach { it.visibility = View.GONE } // Hide all initially
            order.items.take(3).forEachIndexed { index, item ->
                images[index].visibility = View.VISIBLE
                images[index].load(item.imageUrl)
            }

            itemView.setOnClickListener { onItemClick(order) }
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
