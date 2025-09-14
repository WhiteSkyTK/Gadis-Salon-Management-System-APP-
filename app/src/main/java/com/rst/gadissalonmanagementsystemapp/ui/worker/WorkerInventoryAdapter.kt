package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerStockBinding

class WorkerInventoryAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<WorkerInventoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productImage.load(product.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.productName.text = product.name

            // Display info from the first variant as a summary
            val firstVariant = product.variants.firstOrNull()
            binding.productSize.text = firstVariant?.size ?: "N/A"
            binding.stockCountText.text = (firstVariant?.stock ?: 0).toString()

            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkerStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}