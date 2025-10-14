package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.ui.booking.BookingAdapter
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingBinding

class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var bookingAdapter: BookingAdapter
    private var bookingsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "BookingFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }


    override fun onStart() {
        super.onStart()
        // Start listening for live updates when the screen is visible
        listenForMyBookings()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is not visible to prevent memory leaks
        bookingsListener?.remove()
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(emptyList(), mainViewModel.allHairstyles.value ?: emptyList()) { booking ->
            val action = BookingFragmentDirections.actionBookingFragmentToBookingDetailCustomerFragment(booking)
            findNavController().navigate(action)
        }
        binding.bookingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }

    private fun listenForMyBookings() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.bookingRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        bookingsListener = FirebaseManager.addCurrentUserBookingsListener { myBookings ->
            if (view == null) return@addCurrentUserBookingsListener

            // --- STOP SHIMMER ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (myBookings.isEmpty()) {
                // Show empty message if there are no bookings
                binding.bookingRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
            } else {
                // Show the list if there are bookings
                binding.bookingRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                bookingAdapter.updateData(myBookings)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}