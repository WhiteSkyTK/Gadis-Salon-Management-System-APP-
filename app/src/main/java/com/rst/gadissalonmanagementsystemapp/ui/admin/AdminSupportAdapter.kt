package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.SupportMessage
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminSupportTicketBinding

class AdminSupportAdapter(
    private var messages: List<SupportMessage>,
    private val onItemClick: (SupportMessage) -> Unit,
    private val onStatusChange: (SupportMessage, String) -> Unit,
    private val onDelete: (SupportMessage) -> Unit
) : RecyclerView.Adapter<AdminSupportAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminSupportTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: SupportMessage) {
            binding.senderNameText.text = "${message.senderName} (${message.senderEmail})"
            binding.messageText.text = message.message
            binding.statusChip.text = message.status

            // Set the visual indicator for "New" messages
            if (message.status.equals("New", ignoreCase = true)) {
                // You'll need to define 'card_background_highlight' in your colors.xml
                binding.root.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_background_highlight))
                binding.markReadButton.visibility = View.VISIBLE
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.cardBackgroundColor))
                binding.markReadButton.visibility = View.GONE // Hide button if already read
            }

            // Handle item click to open the reply screen
            itemView.setOnClickListener {
                // If the message is new, mark it as read when the admin opens it
                if (message.status.equals("New", ignoreCase = true)) {
                    onStatusChange(message, "Read")
                }
                onItemClick(message) // Then navigate to the reply screen
            }

            // Handle explicit button clicks
            binding.markReadButton.setOnClickListener {
                onStatusChange(message, "Read")
            }

            binding.deleteButton.setOnClickListener {
                onDelete(message)
            }
        }
    }

    fun updateData(newMessages: List<SupportMessage>) {
        this.messages = newMessages
        notifyDataSetChanged() // Tell the RecyclerView to redraw itself
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