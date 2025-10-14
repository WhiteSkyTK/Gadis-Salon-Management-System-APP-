package com.rst.gadissalonmanagementsystemapp.ui.chat

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    // The adapter now internally manages a MutableList for safety
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val TAG = "ChatAdapter"

    inner class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.messageText
            binding.senderNameText.text = message.senderName

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.timestampText.text = message.timestamp?.let { sdf.format(it) } ?: "sending..."

            // Align the bubble
            if (message.isSentByUser) {
                (binding.root as LinearLayout).gravity = Gravity.END
                binding.messageBubble.setBackgroundResource(R.drawable.chat_bubble_sent)
                binding.messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                binding.timestampText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white_transparent_70))
                // Set sender name color for sent messages
                binding.senderNameText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white_transparent_70))
            } else {
                (binding.root as LinearLayout).gravity = Gravity.START
                binding.messageBubble.setBackgroundResource(R.drawable.chat_bubble_received)
                binding.messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorPrimary))
                binding.timestampText.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorSecondary))
                // Set sender name color for received messages
                binding.senderNameText.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorSecondary))
            }

            // Read receipt logic now uses the new theme-aware color
            if (message.isSentByUser) {
                binding.readReceiptIcon.visibility = View.VISIBLE
                when (message.status.uppercase()) {
                    "READ" -> {
                        binding.readReceiptIcon.setImageResource(R.drawable.ic_done_all)
                        binding.readReceiptIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.chatReadReceiptColor))
                    }
                    else -> { // "SENT" or "DELIVERED"
                        binding.readReceiptIcon.setImageResource(R.drawable.ic_done)
                        binding.readReceiptIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.white))
                    }
                }
            } else {
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
        val diffCallback = ChatMessageDiffCallback(this.messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Safely clear and update the mutable list
        messages.clear()
        messages.addAll(newMessages)

        diffResult.dispatchUpdatesTo(this) // Efficiently update the UI
    }
}