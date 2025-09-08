package com.rst.gadissalonmanagementsystemapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    // We'll use a mutable list to simulate sending and receiving messages
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadDummyMessages()

        binding.sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Makes the list start from the bottom
            }
            adapter = chatAdapter
        }
    }

    private fun loadDummyMessages() {
        // In a real app, you'd get these from AppData or Firebase
        chatMessages.addAll(listOf(
            ChatMessage("msg1", "booking1", "stylist1", "Sarah", "Hi TK, looking forward to your appointment!", System.currentTimeMillis() - 100000, false),
            ChatMessage("msg2", "booking1", "user1", "TK", "Me too! See you then.", System.currentTimeMillis() - 50000, true)
        ))
        chatAdapter.notifyDataSetChanged()
    }

    private fun sendMessage() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val newMessage = ChatMessage(
                messageId = "msg${chatMessages.size + 1}",
                bookingId = "booking1",
                senderId = "user1",
                senderName = "TK",
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                isSentByUser = true
            )
            // Add the new message to our list and update the UI
            chatMessages.add(newMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            binding.messageInput.setText("")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}