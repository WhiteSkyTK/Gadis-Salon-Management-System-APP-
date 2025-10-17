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
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentBookingConfirmationBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
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

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            populateStylistChips(args.hairstyle.availableStylistIds)
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
        binding.confirmBookingButton.visibility = if (isOffline) View.GONE else View.VISIBLE
    }


    private fun populateStylistChips(availableStylistIds: List<String>) {
        binding.shimmerViewStylists.startShimmer()
        binding.shimmerViewStylists.visibility = View.VISIBLE
        binding.stylistChipGroup.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {

            Log.d(TAG, "Fetching ${availableStylistIds.size} specific stylists from Firebase...")
            val result = FirebaseManager.getStylistsByIds(availableStylistIds)

            // --- STOP SHIMMER ---
            if (!isAdded) return@launch
            binding.shimmerViewStylists.stopShimmer()
            binding.shimmerViewStylists.visibility = View.GONE
            binding.stylistChipGroup.visibility = View.VISIBLE

            if (result.isSuccess) {
                // The result will ONLY contain the workers for this hairstyle
                allAvailableStylists = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Successfully fetched ${allAvailableStylists.size} available stylists.")
                binding.stylistChipGroup.removeAllViews()

                // Add the "Any Available" chip
                val anyChip = layoutInflater.inflate(R.layout.chip_stylist_with_image, binding.stylistChipGroup, false) as Chip
                anyChip.text = "Any Available"
                anyChip.isChipIconVisible = false
                anyChip.isChecked = true
                binding.stylistChipGroup.addView(anyChip)

                // Add a chip for each specific stylist
                allAvailableStylists.forEach { stylistUser ->
                    val chip = layoutInflater.inflate(R.layout.chip_stylist_with_image, binding.stylistChipGroup, false) as Chip
                    chip.text = stylistUser.name
                    chip.tag = stylistUser
                    chip.chipIcon = context?.let {
                        coil.Coil.imageLoader(it).execute(
                            coil.request.ImageRequest.Builder(it)
                                .data(stylistUser.imageUrl.ifEmpty { R.drawable.ic_profile })
                                .transformations(CircleCropTransformation())
                                .target { drawable -> chip.chipIcon = drawable }
                                .build()
                        ).drawable
                    }
                    binding.stylistChipGroup.addView(chip)
                }
                updateAvailableTimeSlots()
            } else {
                Log.e(TAG, "Error fetching stylists: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun setupListeners() {
        binding.stylistChipGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "Chip selection changed. Checked ID is: $checkedId")
            if (checkedId != View.NO_ID) {
                val selectedChip = group.findViewById<Chip>(checkedId)
                selectedStylist = selectedChip?.tag as? User
                Log.d(TAG, "Selected Stylist Object: ${selectedStylist?.name ?: "'Any Available' (null object)"}")
            } else {
                selectedStylist = null // When "Any Available" is re-selected if it's the only one
            }
            selectedTime = null
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
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)
                selectedTime = null
                Log.d(TAG, "Date selected: $selectedDate")
                updateAvailableTimeSlots()
            }
        }
    }

    private fun updateAvailableTimeSlots() {
        val date = selectedDate.takeIf { it.isNotBlank() } ?: return
        val stylist = selectedStylist

        // --- START LOADING STATE ---
        binding.timeSlotLoadingIndicator.visibility = View.VISIBLE
        binding.timeSlotRecyclerView.visibility = View.INVISIBLE
        binding.confirmBookingButton.isEnabled = false // Disable button during load

        Log.d(TAG, "Updating time slots for date: $date and stylist: ${stylist?.name}")

        viewLifecycleOwner.lifecycleScope.launch {
            val masterSlotsResult = FirebaseManager.getSalonTimeSlots()
            if (!masterSlotsResult.isSuccess) {
                Toast.makeText(context, "Error fetching salon hours.", Toast.LENGTH_SHORT).show()
                binding.timeSlotLoadingIndicator.visibility = View.GONE // Hide loading on error
                return@launch
            }
            val masterSlotList = masterSlotsResult.getOrNull() ?: emptyList()
            var availableSlots: List<String>

            if (stylist != null) {
                val result =
                    FirebaseManager.getAvailableSlots(stylist.name, date, args.hairstyle.id)
                availableSlots =
                    if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()
            } else {
                availableSlots = masterSlotList
            }

            // 1. Create a new, changeable (mutable) copy of the availableSlots list.
            val mutableAvailableSlots = availableSlots.toMutableList()

            // 2. Filter for the 3-hour lead time if the selected date is today
            val todayDateString =
                SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
            if (date == todayDateString) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                mutableAvailableSlots.removeAll { slot ->
                    val slotHour = slot.split(":")[0].toInt()
                    slotHour < currentHour + BOOKING_LEAD_TIME_HOURS
                }
            }

            if (isAdded) {
                binding.timeSlotLoadingIndicator.visibility = View.GONE
                binding.timeSlotRecyclerView.visibility = View.VISIBLE

                Log.d(TAG, "Final available slots: $mutableAvailableSlots")
                binding.timeSlotRecyclerView.adapter =
                    TimeSlotAdapter(masterSlotList, mutableAvailableSlots) { time ->
                        selectedTime = time
                        checkIfReadyToBook()
                    }
                checkIfReadyToBook()
            }
        }
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

            Log.d("BookingConfirm", "--- Preparing to Create Booking ---")
            Log.d("BookingConfirm", "Current User UID from Auth: ${Firebase.auth.currentUser?.uid}")
            Log.d("BookingConfirm", "CurrentUser ID from Firestore: ${currentUser.id}")
            Log.d("BookingConfirm", "Booking will have customerId: ${currentUser.id}")
            Log.d("BookingConfirm", "------------------------------------")


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
        // This check is now robust and prevents the crash
        val isReady = !selectedDate.isNullOrEmpty() && !selectedTime.isNullOrEmpty()
        val isLoading = binding.timeSlotLoadingIndicator.visibility == View.VISIBLE
        binding.confirmBookingButton.isEnabled = isReady && !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}