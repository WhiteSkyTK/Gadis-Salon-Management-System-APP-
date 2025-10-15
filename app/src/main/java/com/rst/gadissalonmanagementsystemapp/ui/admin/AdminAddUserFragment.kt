package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.functions.functions
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.DialogConfirmPasswordBinding
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddUserBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class AdminAddUserFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminAddUserBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { takeImage() } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImagePreview.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.profileImagePreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Updated click listener to show the bottom sheet
        binding.profileImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "UserImagePicker")
        }

        binding.saveUserButton.setOnClickListener {
            validateAndShowConfirmation()
        }
    }

    // --- This function is called when an option is selected in the bottom sheet ---
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                binding.profileImagePreview.setImageResource(R.drawable.ic_add_a_photo)
                selectedImageUri = null
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takeImage()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takeImageLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.provider", tmpFile)
    }


    private fun validateAndShowConfirmation() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val role = when (binding.roleRadioGroup.checkedRadioButtonId) {
            binding.radioWorker.id -> "WORKER"
                //binding.radioAdmin.id -> "ADMIN"
            else -> "CUSTOMER"
        }

        // Clear previous errors first
        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.phoneLayout.error = null
        binding.passwordLayout.error = null

        var isValid = true

        if (name.isEmpty()) {
            binding.nameLayout.error = "Name cannot be empty"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            binding.phoneLayout.error = "Please enter a valid 10-digit phone number"
            isValid = false
        }

        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (!isValid) {
            return // Stop if any validation fails
        }

        // Show the password confirmation dialog
        showAdminPasswordDialog { adminPassword ->
            // This code runs after the admin confirms their password
            performSecureUserCreation(name, email, phone, password, role, adminPassword)
        }
    }

    private fun showAdminPasswordDialog(onConfirm: (String) -> Unit) {
        val dialogBinding = DialogConfirmPasswordBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Action")
            .setView(dialogBinding.root)
            .setPositiveButton("Confirm") { _, _ ->
                val enteredPassword = dialogBinding.passwordInput.text.toString()
                onConfirm(enteredPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSecureUserCreation(name: String, email: String, phone: String, password: String, role: String, adminPassword: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveUserButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val adminEmail = Firebase.auth.currentUser?.email
            if (adminEmail == null) {
                Toast.makeText(context, "Could not verify admin session. Please log in again.", Toast.LENGTH_SHORT).show()
                binding.loadingIndicator.visibility = View.GONE
                binding.saveUserButton.isEnabled = true
                return@launch
            }

            // Step 1: Create the new user (this will sign the current admin out)
            val createResult = FirebaseManager.createUserByAdmin(name, email, phone, password, role, selectedImageUri?.toString() ?: "")

            if (createResult.isSuccess) {
                val reLoginResult = FirebaseManager.loginUser(adminEmail, adminPassword)
                if (reLoginResult.isSuccess) {
                    Toast.makeText(context, "$role '$name' added successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(context, "User created, but admin re-login failed. Please log out and back in.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Error creating user: ${createResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }

            binding.loadingIndicator.visibility = View.GONE
            binding.saveUserButton.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}