package com.rst.gadissalonmanagementsystemapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.databinding.ItemProductBinding // Import the binding class for your item layout

class HomeItemAdapter(
    private val items: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<HomeItemAdapter.HomeItemViewHolder>() {

    // This ViewHolder holds the views for a single item.
    // It uses ViewBinding to safely access the views in item_product.xml.
    inner class HomeItemViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productImage.setImageResource(product.imageResId)
            binding.productName.text = product.name
            binding.productDetail.text = product.detail
            // Set the click listener on the whole item view
            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    // This is called when the RecyclerView needs a new ViewHolder (a new row).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeItemViewHolder {
        // We inflate the item_product.xml layout here using its binding class.
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeItemViewHolder(binding)
    }

    // This is called to display the data at a specific position.
    // It gets the product from the list and tells the ViewHolder to bind it.
    override fun onBindViewHolder(holder: HomeItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // This just returns the total number of items in our list.
    override fun getItemCount(): Int {
        return items.size
    }
}