package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminBookingDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter

class AdminBookingDetailFragment : Fragment() {
    private var _binding: FragmentAdminBookingDetailBinding? = null
    private val binding get() = _binding!!
    private val args: AdminBookingDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var chatListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val booking = args.booking

        val hairstyle = mainViewModel.allHairstyles.value?.find { it.id == booking.hairstyleId }
        binding.bookingDetailsInclude.hairstyleImageDetail.load(hairstyle?.imageUrl) { placeholder(R.drawable.ic_placeholder_image) }
        binding.bookingDetailsInclude.serviceNameDetail.text = booking.serviceName
        binding.bookingDetailsInclude.customerNameDetail.text = "Customer: ${booking.customerName}"
        binding.bookingDetailsInclude.stylistNameDetail.text = "Stylist: ${booking.stylistName}"
        binding.bookingDetailsInclude.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        listenForMessages(args.booking.id)
    }

    override fun onStop() {
        super.onStop()
        chatListener?.remove()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenForMessages(bookingId: String) {
        chatListener = FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            if (view != null) {
                val adminUid = Firebase.auth.currentUser?.uid ?: ""
                messages.forEach { it.isSentByUser = (it.senderUid == adminUid) }
                chatAdapter.updateData(messages)
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
