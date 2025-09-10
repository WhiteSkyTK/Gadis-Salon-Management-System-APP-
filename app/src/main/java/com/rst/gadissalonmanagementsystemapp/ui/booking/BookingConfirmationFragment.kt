package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingConfirmationBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

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

        // 2. Fetch stylists and populate chips
        populateStylistChips(hairstyle.availableStylistIds)

        // 3. Setup Listeners
        setupListeners()

        // 4. Populate Time Slots
        binding.timeSlotRecyclerView.adapter = TimeSlotAdapter(AppData.availableTimeSlots)

        // 5. Handle the final confirmation button click
        binding.confirmBookingButton.setOnClickListener {
            confirmBooking()
        }
    }

    private fun populateStylistChips(availableStylistIds: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllUsers()
            if (result.isSuccess) {
                val allUsers = result.getOrNull() ?: emptyList()
                val stylists = allUsers.filter { user ->
                    availableStylistIds.contains(user.id) && user.role == "WORKER"
                }

                binding.stylistChipGroup.removeAllViews()

                // Add the "Any Available" chip first
                val anyChip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                anyChip.text = "Any Available"
                anyChip.isChecked = true
                binding.stylistChipGroup.addView(anyChip)

                // Then, add a chip for each specific stylist
                stylists.forEach { stylistUser ->
                    val chip = layoutInflater.inflate(R.layout.chip_stylist, binding.stylistChipGroup, false) as Chip
                    chip.text = stylistUser.name
                    chip.tag = stylistUser.name // Store the name to be used in the booking
                    binding.stylistChipGroup.addView(chip)
                }
            } else {
                Toast.makeText(context, "Error fetching stylists", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.stylistChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedStylistName = selectedChip?.text?.toString()
            checkIfReadyToBook()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
            checkIfReadyToBook()
        }

        // TODO: Update TimeSlotAdapter to handle clicks and set the selectedTime variable
    }

    private fun confirmBooking() {
        val currentUser = AppData.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(context, "Error: User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedDate)

        val newBooking = AdminBooking(
            id = UUID.randomUUID().toString(),
            serviceName = args.hairstyle.name,
            customerName = currentUser.name,
            stylistName = selectedStylistName ?: "Any Available",
            date = formattedDate,
            time = selectedTime ?: "Any Time",
            status = "Pending"
        )

        AppData.addBooking(newBooking)
        Toast.makeText(context, "Booking Confirmed!", Toast.LENGTH_LONG).show()
        findNavController().navigate(R.id.action_bookingConfirmationFragment_to_bookingSuccessFragment)
    }

    private fun checkIfReadyToBook() {
        binding.confirmBookingButton.isEnabled = (selectedStylistName != null && selectedDate != null)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}