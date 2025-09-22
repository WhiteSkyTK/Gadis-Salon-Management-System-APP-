package com.rst.gadissalonmanagementsystemapp.ui.chat

import androidx.recyclerview.widget.DiffUtil
import com.rst.gadissalonmanagementsystemapp.ChatMessage

class ChatMessageDiffCallback(
    private val oldList: List<ChatMessage>,
    private val newList: List<ChatMessage>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    // Checks if two items are the same entity (e.g., have the same ID)
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // We use the timestamp as a unique identifier, as it's set by the server
        return oldList[oldItemPosition].timestamp == newList[newItemPosition].timestamp
    }

    // Checks if the content of an item has changed (e.g., the status was updated)
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // The data class 'equals' will check if any field, like 'status', has changed.
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}