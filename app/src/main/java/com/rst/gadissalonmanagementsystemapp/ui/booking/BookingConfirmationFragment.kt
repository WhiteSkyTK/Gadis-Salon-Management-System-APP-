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
import java.util.Locale

class BookingConfirmationFragment : Fragment() {

    private var _binding: FragmentBookingConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: BookingConfirmationFragmentArgs by navArgs()

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
            // In a real app, you would save the booking here
            Toast.makeText(context, "Booking Confirmed!", Toast.LENGTH_LONG).show()
            // Navigate to the user's main booking list
            findNavController().navigate(R.id.bookingFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}