package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import coil.load
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditUserBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import java.io.File

class AdminEditUserFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {
    private var _binding: FragmentAdminEditUserBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditUserFragmentArgs by navArgs()
    private var userToEdit: User? = null
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null
    private var imageRemoved = false

    // --- Activity Result Launchers for image picking and permissions ---
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takeImage() else Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            imageRemoved = false
            binding.profileImagePreview.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                imageRemoved = false
                binding.profileImagePreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData(args.userId)

        binding.profileImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "AdminEditUserPic")
        }
        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUserData(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getUser(userId)
            if (result.isSuccess) {
                userToEdit = result.getOrNull()

                // If the user is found, populate the fields
                if (userToEdit != null) {
                    populateFields(userToEdit!!) // We can safely use !! here because of the null check
                } else {
                    Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } else {
                Toast.makeText(context, "Error loading user data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateFields(user: User) {
        binding.profileImagePreview.load(user.imageUrl) {
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }
        binding.emailText.text = user.email
        binding.nameInput.setText(user.name)
        binding.phoneInput.setText(user.phone)

        when (user.role.uppercase()) {
            "WORKER" -> binding.radioWorker.isChecked = true
            //"ADMIN" -> binding.radioAdmin.isChecked = true
            else -> binding.radioCustomer.isChecked = true
        }
    }

    private fun saveChanges() {
        if (userToEdit == null) return

        val newName = binding.nameInput.text.toString().trim()
        val newPhone = binding.phoneInput.text.toString().trim()
        val newRole = when (binding.roleRadioGroup.checkedRadioButtonId) {
            binding.radioWorker.id -> "WORKER"
            //binding.radioAdmin.id -> "ADMIN"
            else -> "CUSTOMER"
        }

        binding.nameLayout.error = null
        binding.phoneLayout.error = null

        if (newName.isEmpty()) {
            binding.nameLayout.error = "Name cannot be empty"
            return
        }

        if (newPhone.length != 10 || !newPhone.all { it.isDigit() }) {
            binding.phoneLayout.error = "Please enter a valid 10-digit phone number"
            return
        }
        // --- SHOW LOADING INDICATOR ---
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveChangesButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var imageUrl = userToEdit!!.imageUrl

            // Handle image upload if a new image was selected
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "profile_pictures", userToEdit!!.id)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Error uploading image.", Toast.LENGTH_SHORT).show()
                    binding.loadingIndicator.visibility = View.GONE
                    binding.saveChangesButton.isEnabled = true
                    return@launch
                }
            } else if (imageRemoved) {
                imageUrl = "" // Set image URL to empty if it was removed
            }

            val updatedUser = userToEdit!!.copy(
                name = newName,
                phone = newPhone,
                role = newRole,
                imageUrl = imageUrl
            )

            val result = FirebaseManager.updateUser(updatedUser)
            if (result.isSuccess) {
                Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            // --- HIDE LOADING INDICATOR on completion/failure ---
            binding.loadingIndicator.visibility = View.GONE
            binding.saveChangesButton.isEnabled = true
        }
    }

    // --- This function is called from the Bottom Sheet ---
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null
                imageRemoved = true
                binding.profileImagePreview.setImageResource(R.drawable.ic_profile)
            }
        }
    }

    // --- Camera and Permission Logic ---
    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> takeImage()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takeImageLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image", ".png", requireContext().cacheDir).apply { createNewFile(); deleteOnExit() }
        return FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.provider", tmpFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}