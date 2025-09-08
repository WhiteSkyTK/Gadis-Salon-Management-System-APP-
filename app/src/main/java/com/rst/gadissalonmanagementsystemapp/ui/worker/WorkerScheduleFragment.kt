package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerScheduleBinding

class WorkerScheduleFragment : Fragment() {
    private var _binding: FragmentWorkerScheduleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scheduleRecyclerViewWorker.layoutManager = LinearLayoutManager(context)

        val currentWorker = AppData.getCurrentUser()

        AppData.allBookings.observe(viewLifecycleOwner) { allBookings ->
            // Show only bookings confirmed by the current worker
            val mySchedule = allBookings.filter {
                it.stylistName == currentWorker?.name && it.status.equals("Confirmed", ignoreCase = true)
            }
            binding.scheduleRecyclerViewWorker.adapter = WorkerScheduleAdapter(mySchedule)
        }

        // TODO: Add logic for the CalendarView to filter the list by date
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}