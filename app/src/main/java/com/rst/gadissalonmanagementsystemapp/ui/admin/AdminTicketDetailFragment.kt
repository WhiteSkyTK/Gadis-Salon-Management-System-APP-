package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminTicketDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch

class AdminTicketDetailFragment : Fragment() {
    private var _binding: FragmentAdminTicketDetailBinding? = null
    private val binding get() = _binding!!
    private val args: AdminTicketDetailFragmentArgs by navArgs()

    private var repliesListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminTicketDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ticket = args.ticket

        // Display original ticket info
        binding.senderInfoText.text = "From: ${ticket.senderName} (${ticket.senderEmail})"
        binding.originalMessageText.text = ticket.message

        if (ticket.status.equals("Closed", ignoreCase = true)) {
            binding.replyInputLayout.visibility = View.GONE
            binding.actionButtonCard.visibility = View.GONE
        }
        setupRecyclerView()

        binding.sendReplyButton.setOnClickListener { sendReply(ticket.id) }

        binding.closeTicketButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Close Ticket")
                .setMessage("Are you sure you want to mark this ticket as resolved?")
                .setPositiveButton("Yes, Close") { _, _ -> closeTicket(ticket.id) }
                .setNegativeButton("Cancel", null)
                .show()
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
        val currentAdmin = Firebase.auth.currentUser
        if (messageText.isNotEmpty() && currentAdmin != null) {
            val replyMessage = ChatMessage(
                senderUid = currentAdmin.uid,
                senderName = "Admin Support",
                messageText = messageText
            )
            val allParticipants = (args.ticket.participantUids + currentAdmin.uid).distinct()

            viewLifecycleOwner.lifecycleScope.launch {
                // Pass the full list of participants to the updated function.
                FirebaseManager.sendSupportReply(ticketId, replyMessage, allParticipants)
                binding.replyInput.setText("")
            }
        }
    }

    private fun closeTicket(ticketId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateSupportTicketStatus(ticketId, "Closed")
            if (result.isSuccess) {
                Toast.makeText(context, "Ticket has been closed.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Failed to close ticket.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}