package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager.updateBookingStatus
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingDetailWorkerBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch
import java.util.UUID

class BookingDetailWorkerFragment : Fragment() {
    private var _binding: FragmentBookingDetailWorkerBinding? = null
    private val binding get() = _binding!!
    private val args: BookingDetailWorkerFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var currentUser: User? = null
    private var chatListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

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
        val hairstyle = mainViewModel.allHairstyles.value?.find { it.id == booking.hairstyleId }
        binding.hairstyleImageDetail.load(hairstyle?.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
        }
        binding.serviceNameDetail.text = booking.serviceName
        binding.customerNameDetail.text = "Customer: ${booking.customerName}"
        binding.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        if (!booking.status.equals("Confirmed", ignoreCase = true)) {
            binding.inputLayout.visibility = View.GONE
            binding.actionButtonsLayout.visibility = View.GONE // Also hide the action buttons
        }

        setupRecyclerView()
        setupActionButtons(booking)
        binding.sendButton.setOnClickListener { sendMessage(booking.id) }

        if (booking.workerUnreadCount > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                FirebaseManager.resetWorkerUnreadCount(booking.id)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        // Start listening for messages when the screen becomes visible
        listenForMessages(args.booking.id)

        // Also mark messages as read when the screen becomes visible
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markMessagesAsRead(args.booking.id)
        }
    }

    override fun onStop() {
        super.onStop()
        chatListener?.remove()
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX ---
        // We apply the same change here for consistency and stability.
        chatAdapter = ChatAdapter(mutableListOf())

        val chatLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = chatLayoutManager
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenForMessages(bookingId: String) {
        // This is now the ONLY place where the listener is created.
        chatListener = FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            if (view != null) {
                val uid = currentUser?.id ?: Firebase.auth.currentUser?.uid ?: ""
                messages.forEach { it.isSentByUser = (it.senderUid == uid) }
                chatAdapter.updateData(messages)
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }



    private fun sendMessage(bookingId: String) {
        val messageText = binding.messageInput.text.toString().trim()
        val currentUser = Firebase.auth.currentUser

        if (messageText.isNotEmpty() && currentUser != null) {
            val message = ChatMessage(
                bookingId = bookingId,
                senderUid = currentUser.uid,
                senderName = currentUser.displayName ?: "Customer",
                messageText = messageText
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val result = FirebaseManager.sendChatMessage(bookingId, message)
                if (result.isSuccess) {
                    binding.messageInput.setText("") // Clear the input field after sending
                } else {
                    Toast.makeText(context, "Failed to send message.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupActionButtons(booking: AdminBooking) {
        //Only show the action buttons if the booking is still 'Confirmed'
        if (booking.status.equals("Confirmed", ignoreCase = true)) {
            binding.actionButtonsLayout.visibility = View.VISIBLE
        } else {
            binding.actionButtonsLayout.visibility = View.GONE
        }

        binding.completeBookingButton.setOnClickListener {
            updateBookingStatus(booking.id, "Completed", "Booking marked as complete!")
        }

        binding.cancelBookingButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    updateBookingStatus(booking.id, "Cancelled", "Booking has been cancelled.")
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun updateBookingStatus(bookingId: String, newStatus: String, successMessage: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateBookingStatus(bookingId, newStatus)
            if (result.isSuccess) {
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Go back to the schedule
            } else {
                Toast.makeText(context, "Failed to update status.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}