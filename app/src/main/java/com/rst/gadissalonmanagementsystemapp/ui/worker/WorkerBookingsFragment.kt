package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerBookingsBinding
import kotlinx.coroutines.launch

class WorkerBookingsFragment : Fragment() {
    private var _binding: FragmentWorkerBookingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WorkerBookingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForPendingBookings()
    }

    private fun setupRecyclerView() {
        adapter = WorkerBookingAdapter(
            bookings = mutableListOf(),
            onAccept = { booking ->
                acceptBooking(booking)
            },
            onDecline = { booking ->
                declineBooking(booking)
            }
        )
        binding.workerBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerBookingsRecyclerView.adapter = adapter
    }

    private fun listenForPendingBookings() {
        FirebaseManager.addPendingBookingsListener { pendingBookings ->
            Log.d("WorkerBookings", "Live update: Found ${pendingBookings.size} pending bookings.")
            adapter.updateData(pendingBookings)
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