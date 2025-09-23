package com.rst.gadissalonmanagementsystemapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.launch
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationsAdapter
    private var notificationListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        notificationListener = FirebaseManager.addUserNotificationsListener { notifications ->
            if (view != null) {
                if (notifications.isEmpty()) {
                    binding.notificationsRecyclerView.visibility = View.GONE
                    binding.emptyNotificationsText.visibility = View.VISIBLE
                } else {
                    binding.notificationsRecyclerView.visibility = View.VISIBLE
                    binding.emptyNotificationsText.visibility = View.GONE
                    adapter.updateData(notifications)
                }
            }
        }

        // As soon as the screen is visible, mark all notifications as read.
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markAllNotificationsAsRead()
        }
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FINAL, COMPLETED LOGIC ---
        adapter = NotificationsAdapter(emptyList()) { notification ->
            // Mark the single notification as read immediately for a responsive feel
            if (!notification.isRead) {
                lifecycleScope.launch { FirebaseManager.markNotificationAsRead(notification.id) }
            }

            // Decide where to navigate based on the notification's data
            when {
                notification.bookingId != null -> {
                    lifecycleScope.launch {
                        val result = FirebaseManager.getBooking(notification.bookingId)
                        if (result.isSuccess && result.getOrNull() != null) {
                            val action = NotificationsFragmentDirections.actionNotificationsFragmentToBookingDetailCustomerFragment(result.getOrNull()!!)
                            findNavController().navigate(action)
                        } else {
                            Toast.makeText(context, "Booking not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                notification.orderId != null -> {
                    lifecycleScope.launch {
                        val result = FirebaseManager.getProductOrder(notification.orderId)
                        if (result.isSuccess && result.getOrNull() != null) {
                            val action = NotificationsFragmentDirections.actionNotificationsFragmentToOrderDetailFragment(result.getOrNull()!!)
                            findNavController().navigate(action)
                        } else {
                            Toast.makeText(context, "Order not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    // Handle general notifications that don't link anywhere
                    Toast.makeText(context, "This is a general notification.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.notificationsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}