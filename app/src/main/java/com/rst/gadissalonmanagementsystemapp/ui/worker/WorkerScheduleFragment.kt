package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerScheduleBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkerScheduleFragment : Fragment() {
    private var _binding: FragmentWorkerScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleAdapter: WorkerScheduleAdapter
    private var allMyBookings = listOf<AdminBooking>()
    private var allHairstyles = listOf<Hairstyle>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForMyScheduleUpdates()

        binding.calendarViewWorker.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(
                Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
            )
            filterScheduleForDate(selectedDate)
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = WorkerScheduleAdapter(emptyList(), allHairstyles) { booking ->
            // This is what happens when a worker clicks an appointment in the list
            val action = WorkerScheduleFragmentDirections.actionWorkerScheduleFragmentToBookingDetailWorkerFragment(booking)
            findNavController().navigate(action)
        }
        binding.scheduleRecyclerViewWorker.layoutManager = LinearLayoutManager(context)
        binding.scheduleRecyclerViewWorker.adapter = scheduleAdapter
    }

    private fun listenForMyScheduleUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            val hairstyleResult = FirebaseManager.getAllHairstyles()
            if (hairstyleResult.isSuccess) {
                allHairstyles = hairstyleResult.getOrNull() ?: emptyList()
            }

            FirebaseManager.addWorkerScheduleListener { myBookings ->
                allMyBookings = myBookings
                // Highlight the calendar dates that have bookings
                highlightBookingDates(myBookings)

                val today = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
                filterScheduleForDate(today)
            }
        }
    }

    private fun highlightBookingDates(bookings: List<AdminBooking>) {
        // TODO: Implement a custom DayViewDecorator for the CalendarView
        // to add a dot or color to dates that are in the bookings list.
    }

    private fun filterScheduleForDate(date: String) {
        val filteredList = allMyBookings.filter { it.date == date }
        scheduleAdapter.updateData(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}