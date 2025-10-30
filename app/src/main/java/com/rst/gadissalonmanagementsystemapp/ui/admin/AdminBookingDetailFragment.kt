package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.AdminMainActivity
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminBookingDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.chat.ChatAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminBookingDetailFragment : Fragment() {
    private var _binding: FragmentAdminBookingDetailBinding? = null
    private val binding get() = _binding!!
    private val args: AdminBookingDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private val db = Firebase.firestore
    private val functions = Firebase.functions
    private val TAG = "AdminBookingDetail"
    private var chatListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var booking: AdminBooking // Store the booking data
    private var allStylists: List<User> = emptyList() // Cache for stylists

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBookingDetailBinding.inflate(inflater, container, false)
        (activity as? AdminMainActivity)?.updateTitle("Booking Details")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        booking = args.booking

        setupViewMode()
        setupRecyclerView()
        setupClickListeners()
        loadStylistsForSpinner() // Load stylists right away for the edit form
    }

    private fun setupViewMode() {
        val hairstyle = mainViewModel.allHairstyles.value?.find { it.id == booking.hairstyleId }
        binding.bookingDetailsInclude.hairstyleImageDetail.load(hairstyle?.imageUrl) { placeholder(R.drawable.ic_placeholder_image) }
        binding.bookingDetailsInclude.serviceNameDetail.text = booking.serviceName
        binding.bookingDetailsInclude.customerNameDetail.text = "Customer: ${booking.customerName}"
        binding.bookingDetailsInclude.stylistNameDetail.text = "Stylist: ${booking.stylistName}"
        binding.bookingDetailsInclude.bookingTimeDetail.text = "On: ${booking.date} at ${booking.time}"

        // --- MODIFIED: Use both reason fields ---
        val reason = if (booking.cancellationReason.isNotBlank()) {
            booking.cancellationReason
        } else {
            booking.declineReason // This field is now in AdminBooking.kt
        }

        if ((booking.status.equals("Cancelled", ignoreCase = true) || booking.status.equals("Declined", ignoreCase = true)) && reason.isNotBlank()) {
            binding.bookingDetailsInclude.cancellationReasonCard.visibility = View.VISIBLE
            binding.bookingDetailsInclude.cancellationReasonText.text = "Reason: $reason"
        } else {
            binding.bookingDetailsInclude.cancellationReasonCard.visibility = View.GONE
        }
    }

    private fun setupEditMode() {
        // Populate edit fields with current booking data
        binding.bookingDateInput.setText(booking.date)
        binding.bookingTimeInput.setText(booking.time)

        // Setup status spinner
        val statuses = listOf("Pending", "Confirmed", "Completed", "Cancelled", "Missed", "Expired")
        // --- MODIFIED: Use android.R.layout.simple_spinner_item ---
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.bookingStatusSpinner.setAdapter(statusAdapter) // Use setAdapter for AutoCompleteTextView

        // --- MODIFIED: Use .setText to set the value ---
        binding.bookingStatusSpinner.setText(booking.status, false)
    }

    private fun loadStylistsForSpinner() {
        lifecycleScope.launch {
            try {
                // --- MODIFIED: Use correct Firebase query ---
                val snapshot = db.collection("users").whereEqualTo("role", "WORKER").get().await()
                allStylists = snapshot.toObjects(User::class.java) // Cache the list
                // --- END MODIFICATION ---

                val stylistNames = allStylists.map { it.name }

                // --- MODIFIED: Use android.R.layout.simple_spinner_item ---
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stylistNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.bookingStylistSpinner.setAdapter(adapter) // Use setAdapter

                // --- MODIFIED: Find stylist name and use .setText ---
                val currentStylistName = allStylists.find { it.id == booking.stylistId }?.name
                if (currentStylistName != null) {
                    binding.bookingStylistSpinner.setText(currentStylistName, false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading stylists for spinner", e)
                Toast.makeText(context, "Failed to load stylists", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.editBookingButton.setOnClickListener { toggleEditMode(true) }
        binding.cancelEditButton.setOnClickListener { toggleEditMode(false) }
        binding.deleteBookingButton.setOnClickListener { deleteBooking() }
        binding.saveBookingButton.setOnClickListener { saveBookingChanges() }

        // --- NEW: Add click listeners for date input ---
        binding.bookingDateInput.setOnClickListener {
            showDatePicker(it as com.google.android.material.textfield.TextInputEditText)
        }
    }

    // --- NEW: Date Picker Dialog ---
    private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        try {
            // Try to parse existing date to pre-set the picker
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val existingDate = sdf.parse(editText.text.toString())
            if (existingDate != null) {
                calendar.time = existingDate
            }
        } catch (e: Exception) {
            // Ignore if parsing fails, just use today's date
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val format = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(format, Locale.US)
            editText.setText(sdf.format(calendar.time))
        }
        DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    private fun toggleEditMode(isEditing: Boolean) {
        binding.bookingDetailsInclude.root.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.bookingEditForm.visibility = if (isEditing) View.VISIBLE else View.GONE

        // Toggle footer buttons
        binding.editBookingButton.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.deleteBookingButton.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.saveBookingButton.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.cancelEditButton.visibility = if (isEditing) View.VISIBLE else View.GONE

        if (isEditing) {
            // Populate the edit form with the latest data when switching to edit mode
            setupEditMode()
        }
    }

    private fun saveBookingChanges() {
        // --- MODIFIED: Get text from AutoCompleteTextViews ---
        val selectedStylistName = binding.bookingStylistSpinner.text.toString()
        val selectedStylist = allStylists.find { it.name == selectedStylistName }
        val newDate = binding.bookingDateInput.text.toString()
        val newTime = binding.bookingTimeInput.text.toString()
        val newStatus = binding.bookingStatusSpinner.text.toString()
        // --- END MODIFICATION ---

        if (selectedStylist == null) {
            Toast.makeText(context, "Please select a valid stylist", Toast.LENGTH_SHORT).show()
            return
        }
        if (newDate.isEmpty() || newTime.isEmpty()) {
            Toast.makeText(context, "Date and Time cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        // --- MODIFIED: Check against statuses list ---
        val statuses = listOf("Pending", "Confirmed", "Completed", "Cancelled", "Missed", "Expired")
        if (!statuses.contains(newStatus)) {
            Toast.makeText(context, "Please select a valid status", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveBookingButton.isEnabled = false
        binding.saveBookingButton.text = "Saving..."

        lifecycleScope.launch {
            try {
                // If moving to "Completed", call the Cloud Function
                if (newStatus == "Completed" && booking.status != "Completed") {
                    val markBookingPaid = functions.getHttpsCallable("markBookingAsPaid")
                    markBookingPaid.call(mapOf("bookingId" to booking.id)).await()
                    Toast.makeText(context, "Booking marked as paid!", Toast.LENGTH_SHORT).show()
                } else {
                    // For any other status change, just update the document
                    val dataToUpdate = mapOf(
                        "stylistId" to selectedStylist.id,
                        "stylistName" to selectedStylist.name,
                        "date" to newDate,
                        "time" to newTime,
                        "status" to newStatus
                    )
                    db.collection("bookings").document(booking.id)
                        .update(dataToUpdate)
                        .await()
                    Toast.makeText(context, "Booking updated!", Toast.LENGTH_SHORT).show()
                }

                // --- MODIFIED: Update local booking object ---
                // We update the 'var' properties of the data class instance
                booking.stylistId = selectedStylist.id
                booking.stylistName = selectedStylist.name
                booking.date = newDate
                booking.time = newTime
                booking.status = newStatus
                // --- END MODIFICATION ---

                setupViewMode() // Refresh the view-only details
                toggleEditMode(false) // Switch back to view mode

            } catch (e: Exception) {
                Log.e(TAG, "Error saving booking changes", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.saveBookingButton.isEnabled = true
                binding.saveBookingButton.text = "Save"
            }
        }
    }

    private fun deleteBooking() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Booking")
            .setMessage("Are you sure you want to permanently delete this booking? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("bookings").document(booking.id).delete().await()
                        Toast.makeText(context, "Booking deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack() // Go back after deleting
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting booking", e)
                        Toast.makeText(context, "Failed to delete booking", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onStart() {
        super.onStart()
        listenForMessages(args.booking.id)
    }

    override fun onStop() {
        super.onStop()
        chatListener?.remove()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun listenForMessages(bookingId: String) {
        chatListener = FirebaseManager.addChatMessagesListener(bookingId) { messages ->
            if (view != null) {
                val customerUid = args.booking.customerId
                messages.forEach {
                    // isSentByUser (right bubble) = TRUE if the sender is NOT the customer.
                    it.isSentByUser = (it.senderUid != customerUid)
                }
                chatAdapter.updateData(messages)
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

