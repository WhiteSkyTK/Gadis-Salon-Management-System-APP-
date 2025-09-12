package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.functions.functions
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
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
            saveUser()
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

    private fun saveUser() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val role = when (binding.roleRadioGroup.checkedRadioButtonId) {
            binding.radioWorker.id -> "WORKER"
            //binding.radioAdmin.id -> "ADMIN"
            else -> "CUSTOMER"
        }

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || password.length < 6) {
            Toast.makeText(context, "Please fill all fields. Password must be at least 6 characters.s", Toast.LENGTH_LONG).show()
            return
        }

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveUserButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var imageUrl = ""
            // First, upload the image if one was selected
            if (selectedImageUri != null) {
                val imageUploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "profile_pictures", "${UUID.randomUUID()}.jpg")
                if (imageUploadResult.isSuccess) {
                    imageUrl = imageUploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Error uploading image: ${imageUploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    binding.loadingIndicator.visibility = View.GONE
                    binding.saveUserButton.isEnabled = true
                    return@launch
                }
            }

            // Now, create the user in Firebase Auth and save to Firestore
            val result = FirebaseManager.createUserByAdmin(requireContext(), name, email, phone, password, role, imageUrl)

            if (result.isSuccess) {
                val newUserId = result.getOrNull() // Get the new user's ID
                if (newUserId != null && role != "CUSTOMER") {
                    // Step 2: If the user is an Admin or Worker, set their custom claim
                    Log.d("AdminAddUser", "User created, now setting role for UID: $newUserId")
                    val functions = Firebase.functions
                    val data = hashMapOf("userId" to newUserId, "role" to role)

                    try {
                        functions.getHttpsCallable("setUserRole").call(data).await()
                        Log.d("AdminAddUser", "Role set successfully for $newUserId")
                    } catch (e: Exception) {
                        Log.e("AdminAddUser", "Failed to set role for $newUserId", e)
                        Toast.makeText(context, "User created but failed to set role: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                Toast.makeText(context, "$role '$name' added successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error creating user: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.loadingIndicator.visibility = View.GONE
                binding.saveUserButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}