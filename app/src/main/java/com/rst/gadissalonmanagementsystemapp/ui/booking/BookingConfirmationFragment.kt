package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
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

    private var allHairstyles = listOf<Hairstyle>()
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
        val hairstyle = args.hairstyle

        //Populate the summary card
        binding.summaryStyleName.text = hairstyle.name
        binding.summaryImage.load(hairstyle.imageUrl)
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.summaryPrice.text = format.format(hairstyle.price)

        loadInitialData()
        setupListeners()

        binding.confirmBookingButton.setOnClickListener {
            confirmBooking()
        }
    }

    private fun loadInitialData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // First, fetch all hairstyle data so we have access to durations
            val hairstyleResult = FirebaseManager.getAllHairstyles()
            if (hairstyleResult.isSuccess) {
                allHairstyles = hairstyleResult.getOrNull() ?: emptyList()
            }
            // Now fetch the stylists and build the UI
            populateStylistChips(args.hairstyle.availableStylistIds)
        }
    }

    private fun populateStylistChips(availableStylistIds: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllUsers()
            if (result.isSuccess) {
                val allUsers = result.getOrNull() ?: emptyList()
                allAvailableStylists  = allUsers.filter { user ->
                    availableStylistIds.contains(user.id) && user.role == "WORKER"
                }

                binding.stylistChipGroup.removeAllViews()

                val anyChip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                anyChip.text = "Any Available"
                anyChip.isChecked = true
                binding.stylistChipGroup.addView(anyChip)

                allAvailableStylists .forEach { stylistUser ->
                    val chip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                    chip.text = stylistUser.name
                    chip.tag = stylistUser // Store the full User object
                    binding.stylistChipGroup.addView(chip)
                }
                updateAvailableTimeSlots() // Initial time slot calculation
            }
        }
    }

    private fun setupListeners() {
        binding.stylistChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedStylist = selectedChip?.tag as? User // Get the full User object from the tag
            selectedTime = null // Reset time selection when stylist changes
            updateAvailableTimeSlots()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            selectedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(calendar.time)
            selectedTime = null // Reset time selection when date changes
            updateAvailableTimeSlots()
        }
    }

    private fun updateAvailableTimeSlots() {
        val date = selectedDate ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val masterSlotsResult = FirebaseManager.getSalonTimeSlots()
            if (!masterSlotsResult.isSuccess) return@launch

            val masterSlotList = masterSlotsResult.getOrNull() ?: emptyList()
            var availableSlots = masterSlotList.toMutableList()

            // If a specific stylist is chosen, filter slots based on their schedule
            if (selectedStylist != null) {
                val bookingsResult = FirebaseManager.getBookingsForStylistOnDate(selectedStylist!!.name, date)
                if (bookingsResult.isSuccess) {
                    val existingBookings = bookingsResult.getOrNull() ?: emptyList()
                    val occupiedSlots = getOccupiedSlots(existingBookings, allHairstyles)
                    availableSlots.removeAll(occupiedSlots)
                }
            }

            // For now, we assume all slots are available
            binding.timeSlotRecyclerView.layoutManager = GridLayoutManager(context, 4)
            binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(masterSlotList, availableSlots) { time ->
                selectedTime = time
                checkIfReadyToBook()
            }
            checkIfReadyToBook()
        }
    }

    private fun getOccupiedSlots(existingBookings: List<AdminBooking>, hairstyles: List<Hairstyle>): Set<String> {
        val occupied = mutableSetOf<String>()
        existingBookings.forEach { booking ->
            val hairstyle = hairstyles.find { it.name == booking.serviceName }
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
        binding.confirmBookingButton.isEnabled = (selectedDate != null && selectedTime != null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}