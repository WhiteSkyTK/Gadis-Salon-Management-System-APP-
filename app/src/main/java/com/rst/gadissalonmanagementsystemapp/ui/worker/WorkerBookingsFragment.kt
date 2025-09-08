package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerBookingsBinding

class WorkerBookingsFragment : Fragment() {
    private var _binding: FragmentWorkerBookingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.workerBookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        AppData.allBookings.observe(viewLifecycleOwner) { allBookings ->
            // Show only bookings with a "Pending" status
            val pendingBookings = allBookings.filter { it.status.equals("Pending", ignoreCase = true) }

            binding.workerBookingsRecyclerView.adapter = WorkerBookingAdapter(
                bookings = pendingBookings,
                onAccept = { booking ->
                    // Get the currently logged in worker
                    val currentWorker = AppData.getCurrentUser()
                    AppData.updateBookingStatus(booking.id, "Confirmed", currentWorker)
                    Toast.makeText(context, "Booking Accepted!", Toast.LENGTH_SHORT).show()
                },
                onDecline = { booking ->
                    AppData.updateBookingStatus(booking.id, "Cancelled", null)
                    Toast.makeText(context, "Booking Declined", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}