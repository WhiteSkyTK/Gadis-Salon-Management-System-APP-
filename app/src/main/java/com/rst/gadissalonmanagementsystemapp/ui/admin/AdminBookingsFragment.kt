package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.Favoritable
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminBookingsBinding

class AdminBookingsFragment : Fragment() {
    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminBookingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForBookingUpdates()
    }

    private fun setupRecyclerView() {
        // Create the adapter once with an empty list
        adapter = AdminBookingAdapter(emptyList())
        binding.bookingsRecyclerViewAdmin.layoutManager = LinearLayoutManager(context)
        binding.bookingsRecyclerViewAdmin.adapter = adapter
    }

    private fun listenForBookingUpdates() {
        // Start listening for real-time updates from Firebase
        FirebaseManager.addBookingsListener { bookingsList ->
            Log.d("AdminBookings", "Live update: Found ${bookingsList.size} total bookings.")
            // When the data changes, update the adapter's list
            adapter.updateData(bookingsList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}