package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.ProductOrder
import com.rst.gadissalonmanagementsystemapp.databinding.ItemCustomerOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerOrdersAdapter(
    private var orders: List<ProductOrder>,
    private val onItemClick: (ProductOrder) -> Unit
) : RecyclerView.Adapter<CustomerOrdersAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCustomerOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: ProductOrder) {
            binding.orderIdText.text = "Order #${order.id.take(8)}" // Show a short ID
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            binding.orderDateText.text = "Placed on: ${sdf.format(Date(order.timestamp))}"
            binding.totalPriceText.text = "Total: ${NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(order.totalPrice)}"
            binding.statusChip.text = order.status

            // Logic to show a button when the order is ready
            if (order.status.equals("Ready for Pickup", ignoreCase = true)) {
                binding.actionButton.visibility = View.VISIBLE
            } else {
                binding.actionButton.visibility = View.GONE
            }
            itemView.setOnClickListener { onItemClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int {
        return orders.size
    }

    fun updateData(newOrders: List<ProductOrder>) {
        this.orders = newOrders
        notifyDataSetChanged()
    }
}
