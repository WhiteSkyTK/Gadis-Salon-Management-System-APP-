package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.ProductOrder
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerOrderBinding

class WorkerOrdersAdapter(
    private val orders: List<ProductOrder>,
    private val onMarkAsReady: (ProductOrder) -> Unit // Click listener for the button
) : RecyclerView.Adapter<WorkerOrdersAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: ProductOrder) {
            binding.customerNameText.text = "Order for: ${order.customerName}"
            binding.itemCountText.text = "${order.items.size} items"

            // Set the click listener for the "Mark as Ready" button
            binding.markReadyButton.setOnClickListener {
                onMarkAsReady(order)
            }
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
}