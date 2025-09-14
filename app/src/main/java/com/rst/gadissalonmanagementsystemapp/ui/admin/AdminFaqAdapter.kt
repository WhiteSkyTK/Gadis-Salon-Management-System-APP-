package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.FaqItem
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminFaqBinding

class AdminFaqAdapter(
    private var faqList: List<FaqItem>,
    private val onDelete: (FaqItem) -> Unit
) : RecyclerView.Adapter<AdminFaqAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminFaqBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FaqItem) {
            binding.faqQuestionAdmin.text = item.question
            binding.deleteButtonFaq.setOnClickListener {
                // Show a confirmation dialog before deleting
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete FAQ")
                    .setMessage("Are you sure you want to delete this question?")
                    .setPositiveButton("Delete") { _, _ ->
                        onDelete(item)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(faqList[position])
    }

    override fun getItemCount(): Int = faqList.size

    fun updateData(newFaqs: List<FaqItem>) {
        this.faqList = newFaqs
        notifyDataSetChanged()
    }
}