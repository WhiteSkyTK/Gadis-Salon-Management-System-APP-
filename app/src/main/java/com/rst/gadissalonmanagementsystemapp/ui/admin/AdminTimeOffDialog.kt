package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.TimeOffRequest
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.DialogAdminTimeOffBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminTimeOffDialog : DialogFragment() {

    private var _binding: DialogAdminTimeOffBinding? = null
    private val binding get() = _binding!!
    private val args: AdminTimeOffDialogArgs by navArgs()
    private lateinit var db: FirebaseFirestore
    private val TAG = "AdminTimeOffDialog"

    private var stylistsMap = mutableMapOf<String, String>() // Map<StylistName, StylistId>
    private var isEditMode = false
    private var existingRequest: TimeOffRequest? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt() // 90% of screen width
            it.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAdminTimeOffBinding.inflate(inflater, container, false)
        db = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingRequest = args.timeOffRequest
        isEditMode = existingRequest != null

        setupUI()
        loadStylists()
        setupClickListeners()
    }

    private fun setupUI() {
        if (isEditMode) {
            binding.timeoffAdminModalTitle.text = "Edit Time Off"
            binding.timeoffAdminSubmitBtn.text = "Save Changes"
            existingRequest?.let {
                // We set the stylist name in the AutoCompleteTextView later
                binding.timeoffAdminStartDate.setText(it.startDate)
                binding.timeoffAdminEndDate.setText(it.endDate)
                binding.timeoffAdminReason.setText(it.reason)
            }
        } else {
            binding.timeoffAdminModalTitle.text = "Create Time Off"
            binding.timeoffAdminSubmitBtn.text = "Create Request"
        }
    }

    private fun loadStylists() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("users").whereEqualTo("role", "WORKER").get().await()
                val stylistNames = mutableListOf<String>()
                stylistsMap.clear()
                for (doc in snapshot.documents) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        stylistNames.add(user.name)
                        stylistsMap[user.name] = user.id // Store name -> ID
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stylistNames)
                binding.timeoffAdminStylist.setAdapter(adapter)

                // If editing, set the stylist name
                if (isEditMode) {
                    binding.timeoffAdminStylist.setText(existingRequest?.stylistName, false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading stylists", e)
                Toast.makeText(context, "Failed to load stylists", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.timeoffAdminStartDate.setOnClickListener { showDatePicker(it as com.google.android.material.textfield.TextInputEditText) }
        binding.timeoffAdminEndDate.setOnClickListener { showDatePicker(it as com.google.android.material.textfield.TextInputEditText) }
        binding.timeoffAdminCancelBtn.setOnClickListener { dismiss() }
        binding.timeoffAdminSubmitBtn.setOnClickListener { validateAndSubmit() }
    }

    private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
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

    private fun validateAndSubmit() {
        binding.timeoffAdminErrorMsg.visibility = View.GONE
        val stylistName = binding.timeoffAdminStylist.text.toString()
        val stylistId = stylistsMap[stylistName]
        val startDate = binding.timeoffAdminStartDate.text.toString()
        val endDate = binding.timeoffAdminEndDate.text.toString()
        val reason = binding.timeoffAdminReason.text.toString().trim()

        if (stylistId == null || stylistName.isEmpty()) {
            showError("Please select a valid stylist.")
            return
        }
        if (startDate.isEmpty() || endDate.isEmpty() || reason.isEmpty()) {
            showError("Please fill in all fields.")
            return
        }
        if (endDate < startDate) {
            showError("End date cannot be before the start date.")
            return
        }

        // Optional: Check if start date is in the past for *new* requests
        if (!isEditMode) {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val startCal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            try {
                startCal.time = sdf.parse(startDate)!!
            } catch (e: Exception) {
                showError("Invalid start date format.")
                return
            }
            if (startCal.before(today)) {
                showError("Start date cannot be in the past for a new request.")
                return
            }
        }

        // Create a map of data to save
        val dataToSave = mutableMapOf<String, Any>(
            "stylistId" to stylistId,
            "stylistName" to stylistName,
            "startDate" to startDate,
            "endDate" to endDate,
            "reason" to reason,
            "status" to "approved" // Admin created/edited requests are auto-approved
        )


        binding.timeoffAdminSubmitBtn.isEnabled = false
        binding.timeoffAdminSubmitBtn.text = "Saving..."

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    // --- Use .update() for edits ---
                    // This will NOT remove the existing timestamp field
                    db.collection("timeOffRequests").document(existingRequest!!.id)
                        .update(dataToSave)
                        .await()
                    Toast.makeText(context, "Request updated", Toast.LENGTH_SHORT).show()
                } else {
                    // --- Add timestamp only for NEW requests ---
                    dataToSave["timestamp"] = FieldValue.serverTimestamp()
                    db.collection("timeOffRequests").add(dataToSave).await()
                    Toast.makeText(context, "Request created", Toast.LENGTH_SHORT).show()
                }
                dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving time off request", e)
                showError("Failed to save request. ${e.message}")
                binding.timeoffAdminSubmitBtn.isEnabled = true
                binding.timeoffAdminSubmitBtn.text = if (isEditMode) "Save Changes" else "Create Request"
            }
        }
    }

    private fun TimeOffRequest.toHashMap(): MutableMap<String, Any?> {
        // Helper to convert data class to map for Firestore, handling nulls
        return mutableMapOf(
            "stylistId" to stylistId,
            "stylistName" to stylistName,
            "startDate" to startDate,
            "endDate" to endDate,
            "reason" to reason,
            "status" to status,
            "timestamp" to timestamp // Will be replaced or removed
        )
    }

    private fun showError(message: String) {
        binding.timeoffAdminErrorMsg.text = message
        binding.timeoffAdminErrorMsg.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
