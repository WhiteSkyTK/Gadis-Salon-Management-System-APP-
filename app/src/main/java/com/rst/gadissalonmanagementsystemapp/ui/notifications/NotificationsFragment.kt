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
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
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
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForNotifications()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun listenForNotifications() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.emptyNotificationsText.visibility = View.GONE

        notificationListener = FirebaseManager.addUserNotificationsListener { notifications ->
            if (view != null) {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE

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

        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markAllNotificationsAsRead()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(emptyList()) { notification ->
            if (!notification.isRead) {
                lifecycleScope.launch { FirebaseManager.markNotificationAsRead(notification.id) }
            }

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