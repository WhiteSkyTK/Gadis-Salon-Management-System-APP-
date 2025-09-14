package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.FaqItem
import com.rst.gadissalonmanagementsystemapp.databinding.ItemFaqBinding

class FaqAdapter(private var faqList: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    inner class FaqViewHolder(private val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(faqItem: FaqItem) {
            binding.faqQuestion.text = faqItem.question
            binding.faqAnswer.text = faqItem.answer

            // Show or hide the answer based on the isExpanded flag
            binding.faqAnswer.visibility = if (faqItem.isExpanded) View.VISIBLE else View.GONE

            // Animate the arrow rotation
            binding.arrowIcon.animate().rotation(if (faqItem.isExpanded) 180f else 0f).setDuration(200).start()

            // Toggle the expanded state when the item is clicked
            itemView.setOnClickListener {
                faqItem.isExpanded = !faqItem.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(faqList[position])
    }

    override fun getItemCount(): Int = faqList.size

    fun updateData(newFaqs: List<FaqItem>) {
        this.faqList = newFaqs
        notifyDataSetChanged() // Tell the RecyclerView to redraw itself
    }
}