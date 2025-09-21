package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingDetailCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val booking = args.booking

        // Find the full hairstyle object from the ViewModel to get its image URL
        val hairstyle = mainViewModel.allHairstyles.value?.find { it.id == booking.hairstyleId }

        binding.hairstyleImageDetail.load(hairstyle?.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
        }
        binding.serviceNameDetail.text = booking.serviceName
        binding.stylistNameDetail.text = "With: ${booking.stylistName}"
        binding.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        setupChat(booking.id)

    }

    override fun onStart() {
        super.onStart()
        // Start listening for messages when the screen becomes visible
        listenForMessages(args.booking.id)
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        chatListener?.remove()
    }

    private fun setupChat(bookingId: String) {
        val chatLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = chatLayoutManager
        binding.chatRecyclerView.adapter = ChatAdapter(emptyList()) // Start with an empty adapter

        binding.sendButton.setOnClickListener {
            sendMessage(bookingId)
        }
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
                messageText = messageText,
                timestamp = System.currentTimeMillis()
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