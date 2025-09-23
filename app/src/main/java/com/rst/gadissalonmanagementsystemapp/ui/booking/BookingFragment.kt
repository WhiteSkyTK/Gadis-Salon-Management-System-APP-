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
        bookingsListener = FirebaseManager.addCurrentUserBookingsListener { myBookings ->
            if (view != null) {
                // --- ADDED LOGS ---
                Log.d(TAG, "Listener callback received with ${myBookings.size} bookings.")
                if (myBookings.isNotEmpty()) {
                    Log.d(TAG, "Updating adapter. First booking status is now '${myBookings[0].status}'")
                }
                bookingAdapter.updateData(myBookings)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}