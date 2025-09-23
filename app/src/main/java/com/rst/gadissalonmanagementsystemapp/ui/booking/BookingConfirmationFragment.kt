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

    private fun populateStylistChips(availableStylistIds: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching ${availableStylistIds.size} specific stylists from Firebase...")
            val result = FirebaseManager.getStylistsByIds(availableStylistIds)
            if (result.isSuccess) {
                // The result will ONLY contain the workers for this hairstyle
                allAvailableStylists = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Successfully fetched ${allAvailableStylists.size} available stylists.")

                binding.stylistChipGroup.removeAllViews()

                // Add the "Any Available" chip
                val anyChip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                anyChip.text = "Any Available"
                anyChip.isChecked = true
                binding.stylistChipGroup.addView(anyChip)

                // Add a chip for each specific stylist
                allAvailableStylists.forEach { stylistUser ->
                    val chip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                    chip.text = stylistUser.name
                    chip.tag = stylistUser
                    binding.stylistChipGroup.addView(chip)
                }
                updateAvailableTimeSlots() // Initial time slot calculation
            } else {
                Log.e(TAG, "Error fetching stylists: ${result.exceptionOrNull()?.message}")
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
        val stylist = selectedStylist

        Log.d(TAG, "Updating time slots for date: $date and stylist: ${stylist?.name}")

        viewLifecycleOwner.lifecycleScope.launch {
            val masterSlotsResult = FirebaseManager.getSalonTimeSlots()
            if (!masterSlotsResult.isSuccess) {
                Toast.makeText(context, "Error fetching salon hours.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val masterSlotList = masterSlotsResult.getOrNull() ?: emptyList()
            var availableSlots: List<String>

            // --- THIS IS THE NEW, SECURE LOGIC ---
            if (stylist != null) {
                // If a specific stylist is chosen, call our secure Cloud Function
                val result = FirebaseManager.getAvailableSlots(stylist.name, date, args.hairstyle.id)
                if (result.isSuccess) {
                    availableSlots = result.getOrNull() ?: emptyList()
                } else {
                    Toast.makeText(context, "Could not fetch availability.", Toast.LENGTH_SHORT).show()
                    availableSlots = emptyList() // Show no slots on error
                }
            } else {
                // If "Any Available" is chosen, show all master slots for now
                availableSlots = masterSlotList
            }

            // Filter for the 3-hour lead time if the selected date is today
            val todayDateString = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
            if (date == todayDateString) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                (availableSlots as MutableList).removeAll { slot ->
                    val slotHour = slot.split(":")[0].toInt()
                    slotHour < currentHour + BOOKING_LEAD_TIME_HOURS
                }
            }

            Log.d(TAG, "Final available slots: $availableSlots")
            binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(masterSlotList, availableSlots) { time ->
                selectedTime = time
                checkIfReadyToBook()
            }
            checkIfReadyToBook()
        }
    }

    private fun getOccupiedSlots(existingBookings: List<AdminBooking>): Set<String> {
        val occupied = mutableSetOf<String>()
        // Safely get the list from the ViewModel. If it's not ready, log an error and stop.
        val allHairstyles = mainViewModel.allHairstyles.value ?: run {
            Log.e(TAG, "Hairstyles list from ViewModel is null. Cannot calculate occupied slots correctly.")
            return occupied // Return empty set if data is not ready
        }
        Log.d(TAG, "Calculating occupied slots with ${allHairstyles.size} total hairstyles available in ViewModel.")

        existingBookings.forEach { booking ->
            val hairstyle = allHairstyles.find { it.name == booking.serviceName }
            // Add a warning if a hairstyle from a booking isn't in our main list
            if (hairstyle == null) {
                Log.w(TAG, "Could not find hairstyle '${booking.serviceName}' in ViewModel to calculate duration. Defaulting to 1 hour.")
            }
            val duration = hairstyle?.durationHours ?: 1 // Default to 1 hour if not found
            val startTimeHour = booking.time.split(":")[0].toInt()

            for (i in 0 until duration) {
                occupied.add(String.format("%02d:00", startTimeHour + i))
            }
        }
        Log.d(TAG, "Calculated occupied slots: $occupied")
        return occupied
    }

    private fun confirmBooking() {
        if (selectedDate.isEmpty() || selectedTime == null) {
            Toast.makeText(context, "Please select a date and time.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val userResult = FirebaseManager.getCurrentUser()
            if (!userResult.isSuccess || userResult.getOrNull() == null) {
                Toast.makeText(context, "Error: You must be logged in to book.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val currentUser = userResult.getOrNull()!!

            val newBooking = AdminBooking(
                hairstyleId = args.hairstyle.id,
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