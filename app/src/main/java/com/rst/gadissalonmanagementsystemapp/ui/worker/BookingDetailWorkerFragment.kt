package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingDetailWorkerBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch
import java.util.UUID

class BookingDetailWorkerFragment : Fragment() {
    private var _binding: FragmentBookingDetailWorkerBinding? = null
    private val binding get() = _binding!!
    private val args: BookingDetailWorkerFragmentArgs by navArgs()
    private var currentUser: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingDetailWorkerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val booking = args.booking

        // Fetch the current user's details
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getCurrentUser()
            if (result.isSuccess) {
                currentUser = result.getOrNull()
            }
        }

        // --- Populate the booking details card ---
        binding.serviceNameDetail.text = booking.serviceName
        binding.customerNameDetail.text = "Customer: ${booking.customerName}"
        binding.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        setupChat(booking.id)

        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markMessagesAsRead(booking.id)
        }
    }

    private fun setupChat(bookingId: String) {
        val chatLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = chatLayoutManager

        FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            val uid = currentUser?.id ?: ""
            // We set the 'isSentByUser' flag here for the UI
            messages.forEach { it.isSentByUser = (it.senderUid  == uid) }
            binding.chatRecyclerView.adapter = ChatAdapter(messages)
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty() && currentUser != null) {
                // Create the message object to be saved to Firebase
                val message = ChatMessage(
                    bookingId = bookingId,
                    senderUid  = currentUser!!.id,
                    senderName = currentUser!!.name,
                    messageText = messageText,
                    timestamp = System.currentTimeMillis()
                )

                // Send the message via FirebaseManager
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = FirebaseManager.sendChatMessage(bookingId, message)
                    if (result.isSuccess) {
                        binding.messageInput.setText("") // Clear the input field
                    } else {
                        Toast.makeText(context, "Failed to send message.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}