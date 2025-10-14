package com.rst.gadissalonmanagementsystemapp.ui.profile

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
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentTicketDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch

class TicketDetailFragment : Fragment() {
    private var _binding: FragmentTicketDetailBinding? = null
    private val binding get() = _binding!!
    private val args: TicketDetailFragmentArgs by navArgs()
    private var repliesListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ticket = args.ticket

        // Display the original ticket info
        binding.originalMessageText.text = ticket.message
        binding.ticketStatusChip.text = "Status: ${ticket.status}"

        // Disable the chat if the ticket is closed or completed
        if (ticket.status.equals("Closed", ignoreCase = true) || ticket.status.equals("Completed", ignoreCase = true)) {
            binding.replyInputLayout.visibility = View.GONE
        }

        if (ticket.status.equals("Closed", ignoreCase = true)) {
            binding.replyInputLayout.visibility = View.GONE
        }

        setupRecyclerView()
        binding.sendReplyButton.setOnClickListener {
            sendReply(ticket.id)
        }
    }

    override fun onStart() {
        super.onStart()
        listenForReplies(args.ticket.id)

        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markSupportRepliesAsRead(args.ticket.id)
        }
    }

    override fun onStop() {
        super.onStop()
        repliesListener?.remove()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mutableListOf())
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.repliesRecyclerView.layoutManager = layoutManager
        binding.repliesRecyclerView.adapter = chatAdapter
    }

    private fun listenForReplies(ticketId: String) {
        repliesListener = FirebaseManager.addSupportTicketRepliesListener(ticketId) { replies ->
            if (view != null) {
                val uid = Firebase.auth.currentUser?.uid ?: ""

                // --- NEW: AUTO-READ LOGIC ---
                // If the list is not empty and the last message is from someone else, mark as read.
                if (replies.isNotEmpty() && replies.last().senderUid != uid) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseManager.markSupportRepliesAsRead(ticketId)
                    }
                }

                replies.forEach { it.isSentByUser = (it.senderUid == uid) }
                chatAdapter.updateData(replies)
                if (replies.isNotEmpty()) {
                    binding.repliesRecyclerView.scrollToPosition(replies.size - 1)
                }
            }
        }
    }

    private fun sendReply(ticketId: String) {
        val messageText = binding.replyInput.text.toString().trim()
        val currentUser = Firebase.auth.currentUser
        if (messageText.isNotEmpty() && currentUser != null) {
            val replyMessage = ChatMessage(
                senderUid = currentUser.uid,
                senderName = currentUser.displayName ?: "User",
                messageText = messageText
            )
            val allParticipants = (args.ticket.participantUids + currentUser.uid).distinct()

            viewLifecycleOwner.lifecycleScope.launch {
                // Pass the full list of participants to the updated function.
                FirebaseManager.sendSupportReply(ticketId, replyMessage, allParticipants)
                binding.replyInput.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}