package com.rst.gadissalonmanagementsystemapp.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.messageText
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.timestampText.text = sdf.format(Date(message.timestamp))

            // Align the bubble
            if (message.isSentByUser) {
                (binding.root as LinearLayout).gravity = Gravity.END
            } else {
                (binding.root as LinearLayout).gravity = Gravity.START
            }

            // --- THIS IS THE NEW READ RECEIPT LOGIC ---
            if (message.isSentByUser) {
                binding.readReceiptIcon.visibility = View.VISIBLE
                when (message.status.uppercase()) {
                    "READ" -> {
                        binding.readReceiptIcon.setImageResource(R.drawable.ic_done_all)
                        binding.readReceiptIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.colorPrimary2))
                    }
                    else -> { // "SENT" or "DELIVERED"
                        binding.readReceiptIcon.setImageResource(R.drawable.ic_done)
                        // Use the default secondary text color for the unread ticks
                        binding.readReceiptIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.textColorSecondary))
                    }
                }
            } else {
                // If it's a received message, hide the icon.
                binding.readReceiptIcon.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<ChatMessage>) {
        this.messages = newMessages
        notifyDataSetChanged()
    }
}