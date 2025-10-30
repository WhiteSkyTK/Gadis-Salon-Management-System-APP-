package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerScheduleBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkerScheduleFragment : Fragment() {
    private var _binding: FragmentWorkerScheduleBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var scheduleAdapter: WorkerScheduleAdapter
    private var allMyBookings = listOf<AdminBooking>()
    private var scheduleListener: ListenerRegistration? = null

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCalendar()
    }

    private fun setupCalendar() {
        binding.calendarViewWorker.setHeaderTextAppearance(R.style.Gadis_Calendar_HeaderText)
        binding.calendarViewWorker.setWeekDayTextAppearance(R.style.Gadis_Calendar_WeekDayText)
        binding.calendarViewWorker.setDateTextAppearance(R.style.Gadis_Calendar_DateText)
        binding.calendarViewWorker.setLeftArrowMask(ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_arrow_left))
        binding.calendarViewWorker.setRightArrowMask(ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_arrow_right))
        binding.calendarViewWorker.selectedDate = CalendarDay.today()

        var isMonthView = true // Start in month view
        binding.toggleCalendarButton.setOnClickListener {
            isMonthView = !isMonthView
            val newMode = if (isMonthView) CalendarMode.MONTHS else CalendarMode.WEEKS
            binding.calendarViewWorker.state().edit().setCalendarDisplayMode(newMode).commit()

            // Animate the arrow icon
            val rotation = if (isMonthView) 180f else 0f
            binding.toggleCalendarButton.animate().rotation(rotation).setDuration(300).start()
        }

        // Initially, the calendar is expanded, so the arrow should point up.
        binding.toggleCalendarButton.rotation = 180f

        binding.calendarViewWorker.setOnDateChangedListener { widget, date, selected ->
            val selectedDateStr = dbDateFormat.format(date.date)
            filterScheduleForDate(selectedDateStr)
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for schedule updates when the screen becomes visible
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForMyScheduleUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        scheduleListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
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
        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.scheduleRecyclerViewWorker.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        scheduleListener = FirebaseManager.addWorkerScheduleListener { myBookings ->
            if (view == null) return@addWorkerScheduleListener

            // --- STOP SHIMMER ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            allMyBookings = myBookings
            highlightBookingDates(myBookings)

            // Re-filter for the currently selected date now that we have new data
            val selectedDate = binding.calendarViewWorker.selectedDate?.date ?: Date()
            val selectedDateStr = dbDateFormat.format(selectedDate)
            filterScheduleForDate(selectedDateStr)
        }
    }

    private fun highlightBookingDates(bookings: List<AdminBooking>) {
        val dateParser = dbDateFormat
        val eventDates = HashSet<CalendarDay>()
        bookings.forEach { booking ->
            try {
                // booking.date is already in "yyyy-MM-dd" format
                val date = dateParser.parse(booking.date)
                if (date != null) {
                    eventDates.add(CalendarDay.from(date))
                }
            } catch (e: Exception) {
                Log.w("WorkerSchedule", "Failed to parse date: ${booking.date}", e)
            }
        }
        binding.calendarViewWorker.removeDecorators()
        binding.calendarViewWorker.addDecorator(EventDayDecorator(eventDates))
    }

    private fun filterScheduleForDate(date: String) {
        val filteredList = allMyBookings.filter { it.date == date }

        // --- NEW LOGIC for empty state ---
        if (filteredList.isEmpty()) {
            binding.scheduleRecyclerViewWorker.visibility = View.GONE
            binding.emptyViewText.visibility = View.VISIBLE
        } else {
            binding.scheduleRecyclerViewWorker.visibility = View.VISIBLE
            binding.emptyViewText.visibility = View.GONE
            scheduleAdapter.updateData(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}