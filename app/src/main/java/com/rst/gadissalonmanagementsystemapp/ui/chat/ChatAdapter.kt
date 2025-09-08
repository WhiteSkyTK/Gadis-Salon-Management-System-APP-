package com.rst.gadissalonmanagementsystemapp.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.databinding.ItemChatMessageBinding

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.messageText

            // Align the bubble left or right based on who sent it
            val params = binding.messageText.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (message.isSentByUser) Gravity.END else Gravity.START
            binding.messageText.layoutParams = params
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
}