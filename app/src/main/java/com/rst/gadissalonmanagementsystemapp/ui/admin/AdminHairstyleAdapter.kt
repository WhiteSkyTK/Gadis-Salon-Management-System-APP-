package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminProductBinding
import java.text.NumberFormat
import java.util.Locale

class AdminHairstyleAdapter(private val items: List<Hairstyle>) : RecyclerView.Adapter<AdminHairstyleAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hairstyle: Hairstyle) {
            binding.itemImage.setImageResource(hairstyle.imageResId)
            binding.itemName.text = hairstyle.name
            // Display the price
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.itemDetail.text = format.format(hairstyle.price)
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