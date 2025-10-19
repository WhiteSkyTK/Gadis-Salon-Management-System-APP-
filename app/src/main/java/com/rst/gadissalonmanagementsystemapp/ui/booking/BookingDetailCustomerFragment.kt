package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.ChatMessage
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingDetailCustomerBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch

class BookingDetailCustomerFragment : Fragment() {

    private var _binding: FragmentBookingDetailCustomerBinding? = null
    private val binding get() = _binding!!
    private val args: BookingDetailCustomerFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var chatListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingDetailCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val booking = args.booking

        // Start the shimmer effect when the view is created
        binding.shimmerViewContainer.startShimmer()

        // Populate the booking details card
        val hairstyle = mainViewModel.allHairstyles.value?.find { it.id == booking.hairstyleId }
        binding.hairstyleImageDetail.load(hairstyle?.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
        }
        binding.serviceNameDetail.text = booking.serviceName
        binding.stylistNameDetail.text = "With: ${booking.stylistName}"
        binding.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        // Check if the booking is cancelled and has a reason
        if (booking.status.equals("Cancelled", ignoreCase = true) || booking.status.equals("Declined", ignoreCase = true)) {
            // Disable the chat for cancelled bookings
            binding.inputLayout.visibility = View.GONE

            // Show the cancellation reason card if a reason exists
            if (booking.cancellationReason.isNotBlank()) {
                binding.cancellationReasonCard.visibility = View.VISIBLE
                binding.cancellationReasonText.text = "Reason: ${booking.cancellationReason}"
            }
        } else if (!booking.status.equals("Confirmed", ignoreCase = true)) {
            // Also disable chat for any other non-confirmed status (Pending, Completed, etc.)
            binding.inputLayout.visibility = View.GONE
        }

        setupRecyclerView()
        binding.sendButton.setOnClickListener { sendMessage(booking.id) }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for messages when the screen becomes visible
        listenForMessages(args.booking.id)
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("CustomerChat", "Screen visible. Marking messages as read for booking: ${args.booking.id}")
            FirebaseManager.markMessagesAsRead(args.booking.id)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        chatListener?.remove()
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX ---
        // We now pass a mutableListOf() which the adapter can correctly modify.
        chatAdapter = ChatAdapter(mutableListOf())

        val chatLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = chatLayoutManager
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenForMessages(bookingId: String) {
        chatListener = FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            if (view != null) {
                // Stop the shimmer and hide it once messages are loaded
                if (binding.shimmerViewContainer.isShimmerVisible) {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                }
                val uid = Firebase.auth.currentUser?.uid ?: ""

                // --- NEW: AUTO-READ LOGIC ---
                // If the list is not empty and the last message is from someone else, mark as read.
                if (messages.isNotEmpty() && messages.last().senderUid != uid) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseManager.markMessagesAsRead(bookingId)
                    }
                }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}