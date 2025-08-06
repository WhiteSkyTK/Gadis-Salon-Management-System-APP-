package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingConfirmationBinding
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class BookingConfirmationFragment : Fragment() {

    private var _binding: FragmentBookingConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: BookingConfirmationFragmentArgs by navArgs()

    private var selectedStylistName: String? = null
    private var selectedDate: Long? = null
    private var selectedTime: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hairstyle = args.hairstyle

        // 1. Populate the summary card
        binding.summaryStyleName.text = hairstyle.name
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.summaryPrice.text = format.format(hairstyle.price)

        // 2. Populate the stylist chips by inflating our new layout
        val availableStylists = AppData.allStylists.filter { hairstyle.availableStylistIds.contains(it.id) }


        // Listen for stylist selection
        binding.stylistChipGroup.setOnCheckedChangeListener { group, checkedId ->
            selectedStylistName = group.findViewById<Chip>(checkedId)?.text?.toString()
            checkIfReadyToBook()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
            checkIfReadyToBook()
        }

        // Populate Available Times from AppData
        binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(AppData.availableTimeSlots)

        // First, clear any existing chips (important for when data might change)
        binding.stylistChipGroup.removeAllViews()

        // Add the "Any Available" chip first
        val anyChip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
        anyChip.text = "Any Available"
        anyChip.isChecked = true
        binding.stylistChipGroup.addView(anyChip)

        // Then, add a chip for each specific stylist
        availableStylists.forEach { stylist ->
            // Inflate our chip_stylist.xml layout
            val chip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip

            // Set the specific text for this stylist
            chip.text = stylist.name

            // Add the newly created chip to the group
            binding.stylistChipGroup.addView(chip)
        }

        // 3. Handle the final confirmation button click (this remains the same)
        binding.confirmBookingButton.setOnClickListener {
            // In a real app, you'd use the selectedStylistName, selectedDate, and selectedTime
            Toast.makeText(context, "Booking Confirmed!", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_bookingConfirmationFragment_to_bookingSuccessFragment)
        }
    }

    private fun checkIfReadyToBook() {
        // Enable the button only if a stylist, date, AND time have been selected.
        // We will add the selectedTime check later when the TimeSlotAdapter is updated.
        binding.confirmBookingButton.isEnabled = (selectedStylistName != null && selectedDate != null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}