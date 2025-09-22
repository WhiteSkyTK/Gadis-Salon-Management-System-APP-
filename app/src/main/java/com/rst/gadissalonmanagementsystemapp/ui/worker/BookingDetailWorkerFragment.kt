package com.rst.gadissalonmanagementsystemapp.ui.worker

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

        setupRecyclerView()
        binding.sendButton.setOnClickListener { sendMessage(booking.id) }

        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markMessagesAsRead(booking.id)
        }
    }


    override fun onStart() {
        super.onStart()
        listenForMessages(args.booking.id)

        // --- THIS IS THE FINAL, POLISHED LOGIC ---
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("WorkerChat", "Screen visible. Marking messages as read for booking: ${args.booking.id}")
            FirebaseManager.markMessagesAsRead(args.booking.id)
        }
    }

    override fun onStop() {
        super.onStop()
        chatListener?.remove()
    }

    private fun setupRecyclerView() {
        // --- THE FIX ---
        // We now pass a mutableListOf() instead of an emptyList()
        chatAdapter = ChatAdapter(mutableListOf())
        val chatLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = chatLayoutManager
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenForMessages(bookingId: String) {
        // We now store the returned listener in our class property
        chatListener = FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            // This check is crucial: only update the UI if the view still exists
            if (view != null) {
                val uid = Firebase.auth.currentUser?.uid ?: ""
                messages.forEach { it.isSentByUser = (it.senderUid == uid) }
                (binding.chatRecyclerView.adapter as ChatAdapter).updateData(messages)
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