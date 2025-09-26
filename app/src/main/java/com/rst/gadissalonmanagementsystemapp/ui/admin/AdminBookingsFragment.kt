package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminBookingsBinding

class AdminBookingsFragment : Fragment() {
    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminBookingAdapter
    private val mainViewModel: MainViewModel by activityViewModels()
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for live updates when the screen is visible
        listenForBookingUpdates()
    }

    override fun onStop() {
        super.onStop()
        // Stop the listener when the screen is not visible
        bookingsListener?.remove()
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX ---
        // We get the hairstyle list from the ViewModel and pass it to the adapter.
        adapter = AdminBookingAdapter(
            bookings = emptyList(),
            allHairstyles = mainViewModel.allHairstyles.value ?: emptyList()
        )
        binding.bookingsRecyclerViewAdmin.layoutManager = LinearLayoutManager(context)
        binding.bookingsRecyclerViewAdmin.adapter = adapter
    }

    private fun listenForBookingUpdates() {
        // Use the real-time listener to keep the list of bookings updated
        bookingsListener = FirebaseManager.addBookingsListener { bookingsList ->
            if (view != null) {
                adapter.updateData(bookingsList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}