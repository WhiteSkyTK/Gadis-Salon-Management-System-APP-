package com.rst.gadissalonmanagementsystemapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rst.gadissalonmanagementsystemapp.NotificationItem
import com.rst.gadissalonmanagementsystemapp.NotificationsAdapter
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Create a list of dummy notifications ---
        val dummyNotifications = listOf(
            NotificationItem(
                title = "Booking Confirmed!",
                body = "Your appointment for Butterfly Locs is confirmed for 15 Aug, 2025.",
                timeAgo = "5 minutes ago",
                iconResId = R.drawable.ic_booking
            ),
            NotificationItem(
                title = "New Product Added",
                body = "Check out the new Eco Style Gel, now available in the shop.",
                timeAgo = "2 hours ago",
                iconResId = R.drawable.ic_shop
            ),
            NotificationItem(
                title = "Payment Successful",
                body = "Your payment of R450 has been received. Thank you!",
                timeAgo = "1 day ago",
                iconResId = R.drawable.ic_payment
            ),
            NotificationItem(
                title = "Upcoming Appointment",
                body = "Just a reminder that you have an appointment tomorrow at 10:30 AM.",
                timeAgo = "3 days ago",
                iconResId = R.drawable.ic_calendar
            )
        )

        // --- 2. Set up the RecyclerView Adapter ---
        val notificationsAdapter = NotificationsAdapter(dummyNotifications)
        binding.notificationsRecyclerView.adapter = notificationsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}