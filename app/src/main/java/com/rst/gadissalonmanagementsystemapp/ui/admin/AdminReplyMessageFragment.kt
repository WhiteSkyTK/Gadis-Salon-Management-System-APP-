package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminReplyMessageBinding
import kotlinx.coroutines.launch

class AdminReplyMessageFragment : Fragment() {
    private var _binding: FragmentAdminReplyMessageBinding? = null
    private val binding get() = _binding!!
    private val args: AdminReplyMessageFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReplyMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = args.supportMessage

        // Display the original message details
        binding.senderInfoText.text = "From: ${message.senderName} (${message.senderEmail})"
        binding.originalMessageText.text = message.message

        // --- THIS IS THE UPDATED CLICK LISTENER ---
        binding.sendReplyButton.setOnClickListener {
            sendReply(message.id)
        }
    }

    private fun sendReply(ticketId: String) {
        val replyText = binding.replyInput.text.toString().trim()
        val currentAdmin = Firebase.auth.currentUser

        if (replyText.isEmpty()) {
            Toast.makeText(context, "Reply cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentAdmin == null) {
            Toast.makeText(context, "You must be logged in as an admin to reply.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.sendReplyButton.isEnabled = false

        // Create the chat message object for the reply
        val replyMessage = ChatMessage(
            bookingId = ticketId, // We can reuse this field to link to the ticket
            senderUid = currentAdmin.uid,
            senderName = "Admin Support", // Use a generic name for replies
            messageText = replyText
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.sendSupportReply(ticketId, replyMessage)
            if (result.isSuccess) {
                Toast.makeText(context, "Reply sent successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Go back to the support list
            } else {
                Toast.makeText(context, "Error sending reply: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.sendReplyButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}