package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminProductBinding

class AdminProductAdapter(private val items: List<Product>) : RecyclerView.Adapter<AdminProductAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.itemImage.setImageResource(product.imageResId)
            binding.itemName.text = product.name
            // Display stock of the first variant
            val stock = product.variants.firstOrNull()?.stock ?: 0
            binding.itemDetail.text = "Stock: $stock"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}