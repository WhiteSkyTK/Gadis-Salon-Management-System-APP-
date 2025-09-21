package com.rst.gadissalonmanagementsystemapp.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

// This adapter works specifically with a List<Hairstyle>
class HairstyleItemAdapter(
    private val items: List<Hairstyle>,
    private val onItemClick: (Hairstyle) -> Unit
) : RecyclerView.Adapter<HairstyleItemAdapter.HairstyleViewHolder>() {

    inner class HairstyleViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hairstyle: Hairstyle) {
            binding.productImage.load(hairstyle.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image) // Optional: show a placeholder while loading
                error(R.drawable.ic_placeholder_image)       // Optional: show an error image if it fails
            }
            binding.productName.text = hairstyle.name

            // Format the price from the Double value
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.productDetail.text = format.format(hairstyle.price)

            itemView.setOnClickListener {
                onItemClick(hairstyle)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HairstyleViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HairstyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HairstyleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}