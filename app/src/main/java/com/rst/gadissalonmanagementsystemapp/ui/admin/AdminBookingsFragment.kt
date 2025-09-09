package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminBookingsBinding

class AdminBookingsFragment : Fragment() {
    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bookingsRecyclerViewAdmin.layoutManager = LinearLayoutManager(context)

        // Observe the central list of all bookings from AppData
        AppData.allBookings.observe(viewLifecycleOwner) { bookingsList ->
            binding.bookingsRecyclerViewAdmin.adapter = AdminBookingAdapter(bookingsList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}