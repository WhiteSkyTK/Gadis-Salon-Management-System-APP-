package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.ProductOrder
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerOrderBinding
import java.text.NumberFormat
import java.util.Locale

class WorkerOrdersAdapter(
    private var orders: MutableList<ProductOrder>,
    private val onItemClick: (ProductOrder) -> Unit, // <-- Add this
    private val onMarkAsReady: (ProductOrder) -> Unit
) : RecyclerView.Adapter<WorkerOrdersAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: ProductOrder) {
            binding.customerNameText.text = "Order for: ${order.customerName}"
            val totalItems = order.items.sumOf { it.quantity }
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemCountText.text = "$totalItems items • Total: ${format.format(order.totalPrice)}"

            // --- Logic to display up to 3 product images ---
            val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3)
            // Hide all images initially
            imageViews.forEach { it.visibility = View.GONE }

            // Show and load images for the first few items in the order
            order.items.take(3).forEachIndexed { index, cartItem ->
                imageViews[index].visibility = View.VISIBLE
                imageViews[index].load(cartItem.imageUrl) {
                    placeholder(R.drawable.ic_placeholder_image)
                    error(R.drawable.ic_placeholder_image)
                }
            }

            // Set the click listener for the button
            binding.markReadyButton.setOnClickListener {
                onMarkAsReady(order)
            }

            itemView.setOnClickListener { onItemClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkerOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int {
        return orders.size
    }

    fun updateData(newOrders: List<ProductOrder>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged() // Tells the RecyclerView to redraw itself
    }
}