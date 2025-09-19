package com.rst.gadissalonmanagementsystemapp.ui.booking

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingConfirmationBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookingConfirmationFragment : Fragment() {

    private var _binding: FragmentBookingConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: BookingConfirmationFragmentArgs by navArgs()

    private val mainViewModel: MainViewModel by activityViewModels()

    companion object {
        private const val TAG = "BookingConfirmation"
        private const val BOOKING_LEAD_TIME_HOURS = 3
    }

    //private var allHairstyles = listOf<Hairstyle>()
    private var allAvailableStylists = listOf<User>()
    private var selectedStylist: User? = null
    private var selectedDate: String = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
    private var selectedTime: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate the static summary info
        binding.summaryStyleName.text = args.hairstyle.name
        binding.summaryImage.load(args.hairstyle.imageUrl)
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.summaryPrice.text = format.format(args.hairstyle.price)

        populateStylistChips(args.hairstyle.availableStylistIds)
        setupListeners()
        binding.confirmBookingButton.setOnClickListener {
            confirmBooking()
        }
    }


/*
    private fun loadInitialData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // First, fetch all hairstyle data so we have access to durations
            val hairstyleResult = FirebaseManager.getAllHairstyles()
            if (hairstyleResult.isSuccess) {
                (activity as? CustomerMainActivity)?.mainViewModel?.allHairstyles?.value = hairstyleResult.getOrNull()
            }
            // Now fetch the stylists and build the UI
            populateStylistChips(args.hairstyle.availableStylistIds)
        }
    }
*/

    private fun populateStylistChips(availableStylistIds: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching all users to find stylists...")
            val result = FirebaseManager.getAllUsers()
            if (result.isSuccess) {
                val allUsers = result.getOrNull() ?: emptyList()
                allAvailableStylists = allUsers.filter { user ->
                    availableStylistIds.contains(user.id) && user.role == "WORKER"
                }
                Log.d(TAG, "Found ${allAvailableStylists.size} available stylists for this service.")

                binding.stylistChipGroup.removeAllViews()
                val anyChip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                anyChip.text = "Any Available"
                anyChip.isChecked = true
                binding.stylistChipGroup.addView(anyChip)

                allAvailableStylists.forEach { stylistUser ->
                    val chip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                    chip.text = stylistUser.name
                    chip.tag = stylistUser
                    binding.stylistChipGroup.addView(chip)
                }
                updateAvailableTimeSlots() // Initial time slot calculation
            }
        }
    }

    private fun setupListeners() {
        binding.stylistChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedStylist = selectedChip?.tag as? User
            selectedTime = null
            Log.d(TAG, "Stylist selected: ${selectedStylist?.name ?: "Any Available"}")
            updateAvailableTimeSlots()
        }
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            // Clear time part for accurate date comparison
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (selectedCalendar.before(today)) {
                Toast.makeText(context, "Cannot book appointments in the past.", Toast.LENGTH_SHORT).show()
                // Clear the time slots
                binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(emptyList(), emptyList()) {}
                selectedDate = "" // Clear date to invalidate booking
                checkIfReadyToBook()
            } else {
                selectedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedCalendar.time)
                selectedTime = null
                Log.d(TAG, "Date selected: $selectedDate")
                updateAvailableTimeSlots()
            }
        }
    }

    private fun updateAvailableTimeSlots() {
        val date = selectedDate.takeIf { it.isNotBlank() } ?: return
        Log.d(TAG, "Updating time slots for date: $date and stylist: ${selectedStylist?.name}")

        viewLifecycleOwner.lifecycleScope.launch {
            val masterSlotsResult = FirebaseManager.getSalonTimeSlots()
            if (!masterSlotsResult.isSuccess) return@launch

            val masterSlotList = masterSlotsResult.getOrNull() ?: emptyList()
            var availableSlots = masterSlotList.toMutableList()

            val todayDateString = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
            if (date == todayDateString) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                // Remove all time slots that are too soon
                availableSlots.removeAll { slot ->
                    val slotHour = slot.split(":")[0].toInt()
                    slotHour < currentHour + BOOKING_LEAD_TIME_HOURS
                }
                Log.d(TAG, "Today is selected. After lead-time filter, available slots: $availableSlots")
            }

            if (selectedStylist != null) {
                val bookingsResult = FirebaseManager.getBookingsForStylistOnDate(selectedStylist!!.name, date)
                if (bookingsResult.isSuccess) {
                    val existingBookings = bookingsResult.getOrNull() ?: emptyList()
                    val occupiedSlots = getOccupiedSlots(existingBookings)
                    availableSlots.removeAll(occupiedSlots)
                }
            }

            binding.timeSlotRecyclerView.layoutManager = GridLayoutManager(context, 4)
            binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(masterSlotList, availableSlots) { time ->
                selectedTime = time
                checkIfReadyToBook()
            }
            checkIfReadyToBook()
        }
    }

    private fun getOccupiedSlots(existingBookings: List<AdminBooking>): Set<String> {
        val occupied = mutableSetOf<String>()
        val allHairstyles = mainViewModel.allHairstyles.value ?: emptyList()
        Log.d(TAG, "Calculating occupied slots with ${allHairstyles.size} total hairstyles available in ViewModel.")

        existingBookings.forEach { booking ->
            val hairstyle = allHairstyles.find { it.name == booking.serviceName }
            val duration = hairstyle?.durationHours ?: 1
            val startTimeHour = booking.time.split(":")[0].toInt()

            for (i in 0 until duration) {
                occupied.add(String.format("%02d:00", startTimeHour + i))
            }
        }
        return occupied
    }

    private fun confirmBooking() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userResult = FirebaseManager.getCurrentUser()
            if (!userResult.isSuccess || userResult.getOrNull() == null) {
                Toast.makeText(context, "Error: You must be logged in to book.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val currentUser = userResult.getOrNull()!!

            val newBooking = AdminBooking(
                serviceName = args.hairstyle.name,
                customerId = currentUser.id,
                customerName = currentUser.name,
                stylistName = selectedStylist?.name ?: "Any Available",
                date = selectedDate,
                time = selectedTime!!,
                status = "Pending"
            )

            val createResult = FirebaseManager.createBooking(newBooking)
            if (createResult.isSuccess) {
                findNavController().navigate(R.id.action_bookingConfirmationFragment_to_bookingSuccessFragment)
            } else {
                Toast.makeText(context, "Booking failed: ${createResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkIfReadyToBook() {
        binding.confirmBookingButton.isEnabled = (!selectedDate.isNullOrEmpty() && !selectedTime.isNullOrEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}