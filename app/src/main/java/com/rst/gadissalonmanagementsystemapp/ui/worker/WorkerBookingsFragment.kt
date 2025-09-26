package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerBookingsBinding
import kotlinx.coroutines.launch


class WorkerBookingsFragment : Fragment() {
    private var _binding: FragmentWorkerBookingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WorkerBookingAdapter
    private val mainViewModel: MainViewModel by activityViewModels()
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        listenForPendingBookings()
    }

    override fun onStop() {
        super.onStop()
        bookingsListener?.remove()
    }

    private fun setupRecyclerView() {
        adapter = WorkerBookingAdapter(
            bookings = mutableListOf(),
            allHairstyles = mainViewModel.allHairstyles.value ?: emptyList(),
            onAccept = { booking ->
                acceptBooking(booking)
            },
            onDecline = { booking ->
                declineBooking(booking)
            },
            onItemClick = { booking ->
                val action = WorkerBookingsFragmentDirections.actionNavWorkerBookingsToBookingDetailWorkerFragment(booking)
                findNavController().navigate(action)
            }
        )
        binding.workerBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerBookingsRecyclerView.adapter = adapter
    }

    private fun listenForPendingBookings() {
        bookingsListener = FirebaseManager.addPendingBookingsListener { pendingBookings ->
            if (view != null) {
                Log.d("WorkerBookings", "Live update: Found ${pendingBookings.size} pending bookings.")
                adapter.updateData(pendingBookings)
            }
        }
    }

    private fun acceptBooking(booking: AdminBooking) {
        viewLifecycleOwner.lifecycleScope.launch {
            val workerResult = FirebaseManager.getCurrentUser()
            if (workerResult.isSuccess && workerResult.getOrNull() != null) {
                val currentWorker = workerResult.getOrNull()!!
                val result = FirebaseManager.acceptBooking(booking.id, currentWorker)
                if (result.isSuccess) {
                    Toast.makeText(context, "Booking Accepted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to accept booking.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error: Could not identify current worker.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun declineBooking(booking: AdminBooking) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateBookingStatus(booking.id, "Declined")
            if (result.isSuccess) {
                Toast.makeText(context, "Booking Declined", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to decline booking.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}