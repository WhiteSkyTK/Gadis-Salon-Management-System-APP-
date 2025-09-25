package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminProductBinding

class AdminProductAdapter(
    private var items: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.itemImage.load(product.imageUrl) { placeholder(R.drawable.ic_placeholder_image) }
            binding.itemName.text = product.name
            val stock = product.variants.firstOrNull()?.stock ?: 0
            binding.itemDetail.text = "Stock: $stock"

            binding.editButton.setOnClickListener { onEditClick(product) }
            binding.deleteButton.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete ${product.name}?")
                    .setPositiveButton("Delete") { _, _ -> onDeleteClick(product) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
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

    fun updateData(newProducts: List<Product>) {
        this.items = newProducts
        notifyDataSetChanged()
    }
}