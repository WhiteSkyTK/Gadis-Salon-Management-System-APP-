package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.DialogWorkerTimeOffBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkerTimeOffDialog : DialogFragment() {

    private var _binding: DialogWorkerTimeOffBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val TAG = "WorkerTimeOffDialog"
    private var currentUser: User? = null

    // Set dialog to be full-width
    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            it.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWorkerTimeOffBinding.inflate(inflater, container, false)
        db = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUser()
        setupClickListeners()
    }

    private fun loadCurrentUser() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "Error: Not logged in", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        lifecycleScope.launch {
            val result = FirebaseManager.getUser(uid)
            if (result.isSuccess) {
                currentUser = result.getOrNull()
                if (currentUser == null) {
                    Toast.makeText(context, "Error: Could not load user data", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } else {
                Toast.makeText(context, "Error: Could not load user data", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun setupClickListeners() {
        binding.timeoffWorkerStartDate.setOnClickListener { showDatePicker(it as com.google.android.material.textfield.TextInputEditText) }
        binding.timeoffWorkerEndDate.setOnClickListener { showDatePicker(it as com.google.android.material.textfield.TextInputEditText) }
        binding.timeoffWorkerCancelBtn.setOnClickListener { dismiss() }
        binding.timeoffWorkerSubmitBtn.setOnClickListener { validateAndSubmit() }
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
        val localCurrentUser = currentUser
        if (localCurrentUser == null) {
            showError("Error: User data not loaded. Cannot submit.")
            return
        }

        binding.timeoffWorkerErrorMsg.visibility = View.GONE
        val startDate = binding.timeoffWorkerStartDate.text.toString()
        val endDate = binding.timeoffWorkerEndDate.text.toString()
        val reason = binding.timeoffWorkerReason.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty() || reason.isEmpty()) {
            showError("Please fill in all fields.")
            return
        }
        if (endDate < startDate) {
            showError("End date cannot be before the start date.")
            return
        }

        // Check if start date is in the past
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
        val startCal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            startCal.time = sdf.parse(startDate)!!
        } catch (e: Exception) {
            showError("Invalid start date format."); return
        }
        if (startCal.before(today)) {
            showError("Start date cannot be in the past.")
            return
        }

        val requestData = mapOf(
            "stylistId" to localCurrentUser.id,
            "stylistName" to localCurrentUser.name,
            "startDate" to startDate,
            "endDate" to endDate,
            "reason" to reason,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        binding.timeoffWorkerSubmitBtn.isEnabled = false
        binding.timeoffWorkerSubmitBtn.text = "Submitting..."

        lifecycleScope.launch {
            try {
                db.collection("timeOffRequests").add(requestData).await()
                Toast.makeText(context, "Request submitted", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving time off request", e)
                showError("Failed to save request. ${e.message}")
                binding.timeoffWorkerSubmitBtn.isEnabled = true
                binding.timeoffWorkerSubmitBtn.text = "Submit Request"
            }
        }
    }

    private fun showError(message: String) {
        binding.timeoffWorkerErrorMsg.text = message
        binding.timeoffWorkerErrorMsg.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
