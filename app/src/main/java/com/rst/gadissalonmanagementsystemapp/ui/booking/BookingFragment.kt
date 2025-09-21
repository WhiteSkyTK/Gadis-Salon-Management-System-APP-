package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.ui.booking.BookingAdapter
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingBinding


class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForMyBookings()
    }

    private fun setupRecyclerView() {
        // Create the adapter, passing an empty hairstyle list for now
        bookingAdapter = BookingAdapter(emptyList(), mainViewModel.allHairstyles.value ?: emptyList()) { booking ->
            // This is what happens when a user clicks on a booking
            val action = BookingFragmentDirections.actionBookingFragmentToBookingDetailCustomerFragment(booking)
            findNavController().navigate(action)
        }
        binding.bookingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }

    private fun listenForMyBookings() {
        // Start listening for real-time updates to the current user's bookings
        FirebaseManager.addCurrentUserBookingsListener { myBookings ->
            // When the data changes, update the adapter's list
            bookingAdapter.updateData(myBookings)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}