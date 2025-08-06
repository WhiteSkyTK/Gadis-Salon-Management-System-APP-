package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.Booking
import com.rst.gadissalonmanagementsystemapp.BookingAdapter
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingBinding

class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Create a list of dummy bookings with different statuses ---
        val dummyBookings = listOf(
            Booking("Butterfly Locs", "Sarah", "15 Aug, 2025", "10:30 AM", "Confirmed"),
            Booking("Dreadlocks Retwist", "Rinae Magadagela", "22 Jul, 2025", "02:00 PM", "Completed"),
            Booking("Cornrows", "Jane", "18 Jun, 2025", "11:00 AM", "Cancelled"),
            Booking("Wash and Set", "Sarah", "05 May, 2025", "09:00 AM", "Completed")
        )

        // Create an instance of our new adapter
        val bookingAdapter = BookingAdapter(dummyBookings)

        // Set up the RecyclerView
        binding.bookingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}