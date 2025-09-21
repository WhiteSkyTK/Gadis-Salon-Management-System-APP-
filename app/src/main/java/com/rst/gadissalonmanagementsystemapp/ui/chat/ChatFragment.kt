package com.rst.gadissalonmanagementsystemapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentChatBinding
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()

    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bookingId = args.bookingId

        setupRecyclerView()
        listenForMessages(bookingId)

        binding.sendButton.setOnClickListener {
            sendMessage(bookingId)
        }
    }

    private fun setupRecyclerView() {
        // Create the adapter once with an empty list
        chatAdapter = ChatAdapter(emptyList())
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun listenForMessages(bookingId: String) {
        // Start listening for real-time updates to this specific chat
        FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            val uid = Firebase.auth.currentUser?.uid ?: ""
            messages.forEach { it.isSentByUser = (it.senderUid == uid) }
            chatAdapter.updateData(messages) // We will add this to the adapter
            binding.chatRecyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage(bookingId: String) {
        val messageText = binding.messageInput.text.toString().trim()
        val currentUser = Firebase.auth.currentUser

        if (messageText.isNotEmpty() && currentUser != null) {
            val message = ChatMessage(
                bookingId = bookingId,
                senderUid = currentUser.uid,
                senderName = currentUser.displayName ?: "User",
                messageText = messageText,
                timestamp = System.currentTimeMillis()
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val result = FirebaseManager.sendChatMessage(bookingId, message)
                if (result.isSuccess) {
                    binding.messageInput.setText("")
                } else {
                    Toast.makeText(context, "Failed to send message.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}