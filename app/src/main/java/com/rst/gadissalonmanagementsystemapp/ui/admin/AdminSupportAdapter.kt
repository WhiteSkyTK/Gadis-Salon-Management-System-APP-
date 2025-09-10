package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.SupportMessage
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminSupportTicketBinding

class AdminSupportAdapter(
    private val messages: List<SupportMessage>,
    private val onItemClick: (SupportMessage) -> Unit, // New parameter for item clicks
    private val onStatusChange: (SupportMessage, String) -> Unit,
    private val onDelete: (SupportMessage) -> Unit
) : RecyclerView.Adapter<AdminSupportAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminSupportTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: SupportMessage) {
            binding.senderNameText.text = "${message.senderName} (${message.senderEmail})"
            binding.messageText.text = message.message
            binding.statusChip.text = message.status

            if (message.status.equals("New", ignoreCase = true)) {
                // You'll need to define 'card_background_highlight' in your colors.xml
                binding.root.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_background_highlight))
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.cardBackgroundColor))
            }

            // Set click listeners
            binding.markReadButton.setOnClickListener { onStatusChange(message, "Read") }
            binding.deleteButton.setOnClickListener { onDelete(message) }
            itemView.setOnClickListener { onItemClick(message) } // Make the whole item clickable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Use the correct binding: ItemAdminSupportTicketBinding
        val binding = ItemAdminSupportTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the data from the 'messages' list
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int {
        // Return the size of the 'messages' list
        return messages.size
    }
}