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
import com.rst.gadissalonmanagementsystemapp.User
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
                // In a real app, you'd set the status to "Declined"
                // For now, let's just delete it for simplicity
                viewLifecycleOwner.lifecycleScope.launch {
                    FirebaseManager.deleteBooking(booking.id) // We will add this function
                    Toast.makeText(context, "Booking Declined", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.workerBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerBookingsRecyclerView.adapter = adapter
    }

    private fun listenForPendingBookings() {
        FirebaseManager.addPendingBookingsListener { pendingBookings ->
            Log.d("WorkerBookings", "Found ${pendingBookings.size} pending bookings.")
            adapter.updateData(pendingBookings)
        }
    }

    private fun acceptBooking(booking: AdminBooking) {
        viewLifecycleOwner.lifecycleScope.launch {
            // First, fetch the current worker's full profile
            val workerResult = FirebaseManager.getCurrentUser()
            if (workerResult.isSuccess) {
                val currentWorker = workerResult.getOrNull()

                if (currentWorker != null) {
                    val result = FirebaseManager.acceptBooking(booking.id, currentWorker)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Booking Accepted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to accept booking.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error: Could not identify current worker.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error: Could not fetch worker profile.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}