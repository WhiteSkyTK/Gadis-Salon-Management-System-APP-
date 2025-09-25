package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminHairstyleBinding
import java.text.NumberFormat
import java.util.Locale

class AdminHairstyleAdapter(
    private var items: List<Hairstyle>,
    private val onEditClick: (Hairstyle) -> Unit,
    private val onDeleteClick: (Hairstyle) -> Unit
) : RecyclerView.Adapter<AdminHairstyleAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminHairstyleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hairstyle: Hairstyle) {
            binding.itemImage.load(hairstyle.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }
            binding.itemName.text = hairstyle.name
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

            binding.itemDetail.text = format.format(hairstyle.price)
            binding.editButton.setOnClickListener { onEditClick(hairstyle) }
            binding.deleteButton.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Hairstyle")
                    .setMessage("Are you sure you want to delete ${hairstyle.name}?")
                    .setPositiveButton("Delete") { _, _ -> onDeleteClick(hairstyle) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminHairstyleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newHairstyles: List<Hairstyle>) {
        this.items = newHairstyles
        notifyDataSetChanged()
    }
}