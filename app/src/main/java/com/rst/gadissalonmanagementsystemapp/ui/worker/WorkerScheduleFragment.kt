package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerScheduleBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkerScheduleFragment : Fragment() {
    private var _binding: FragmentWorkerScheduleBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var scheduleAdapter: WorkerScheduleAdapter
    private var allMyBookings = listOf<AdminBooking>()
    private var scheduleListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.calendarViewWorker.setOnDateChangedListener { widget, date, selected ->
            val selectedDateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(date.date)
            filterScheduleForDate(selectedDateStr)
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for schedule updates when the screen becomes visible
        listenForMyScheduleUpdates()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        scheduleListener?.remove()
    }


    private fun setupRecyclerView() {
        scheduleAdapter = WorkerScheduleAdapter(emptyList(), mainViewModel.allHairstyles.value ?: emptyList()) { booking ->
            val action = WorkerScheduleFragmentDirections.actionWorkerScheduleFragmentToBookingDetailWorkerFragment(booking)
            findNavController().navigate(action)
        }
        binding.scheduleRecyclerViewWorker.layoutManager = LinearLayoutManager(context)
        binding.scheduleRecyclerViewWorker.adapter = scheduleAdapter
    }

    private fun listenForMyScheduleUpdates() {
        // We now store the returned listener in our class property
        scheduleListener = FirebaseManager.addWorkerScheduleListener { myBookings ->
            if (view != null) { // Only update if the view still exists
                allMyBookings = myBookings
                highlightBookingDates(myBookings)

                val selectedDate = binding.calendarViewWorker.selectedDate?.date
                val selectedDateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedDate ?: Date())
                filterScheduleForDate(selectedDateStr)
            }
        }
    }

    private fun highlightBookingDates(bookings: List<AdminBooking>) {
        val dateParser = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val eventDates = HashSet<CalendarDay>()
        bookings.forEach { booking ->
            try {
                val date = dateParser.parse(booking.date)
                if (date != null) {
                    eventDates.add(CalendarDay.from(date))
                }
            } catch (e: Exception) { /* Ignore parse errors */ }
        }
        binding.calendarViewWorker.addDecorator(EventDayDecorator(eventDates))
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