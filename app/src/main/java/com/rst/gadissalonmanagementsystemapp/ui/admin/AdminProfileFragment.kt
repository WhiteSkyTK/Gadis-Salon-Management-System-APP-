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
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminProfileBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import java.io.File

class AdminProfileFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!
    private var latestTmpUri: Uri? = null
    private val TAG = "AdminProfileFragment"

    // --- Activity Result Launchers for image picking and permissions ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { takeImage() } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleImageSelection(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                handleImageSelection(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAdminProfile()
        setupClickListeners()
    }

    private fun handleImageSelection(imageUri: Uri) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            Toast.makeText(context, "Uploading photo...", Toast.LENGTH_SHORT).show()
            val uploadResult = FirebaseManager.uploadImage(imageUri, "profile_pictures", "$uid.jpg")
            if (uploadResult.isSuccess) {
                val downloadUrl = uploadResult.getOrNull().toString()
                val updateResult = FirebaseManager.updateUserProfileImage(uid, downloadUrl)
                if (updateResult.isSuccess) {
                    binding.profileImageAdmin.setImageURI(imageUri)
                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error updating profile: ${updateResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Error uploading photo: ${uploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadAdminProfile() {
        val firebaseUser = Firebase.auth.currentUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getUser(firebaseUser.uid)
            if (result.isSuccess) {
                val adminUser = result.getOrNull()
                binding.userNameAdmin.text = adminUser?.name ?: "Admin"
                binding.userPhoneAdmin.text = adminUser?.phone ?: "No phone number"
                binding.profileImageAdmin.load(adminUser?.imageUrl) {
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                Log.e(TAG, "Failed to load admin details: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun setupClickListeners() {
        binding.profileImageAdmin.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "AdminProfilePicturePicker")
        }

        // --- ADDED NAVIGATION FOR ALL OPTIONS ---
        binding.contactSupportOption.setOnClickListener {
            findNavController().navigate(R.id.action_adminProfileFragment_to_adminSupportFragment)
        }
        binding.settingsOption.setOnClickListener {
            findNavController().navigate(R.id.action_adminProfileFragment_to_adminSettingsFragment)
        }
        binding.locationOption.setOnClickListener {
            findNavController().navigate(R.id.action_adminProfileFragment_to_adminLocationFragment)
        }
        binding.aboutUsOption.setOnClickListener {
            findNavController().navigate(R.id.action_adminProfileFragment_to_adminAboutUsFragment)
        }

        // TODO: Add click listener for the edit button
        binding.editProfileButton.setOnClickListener {
            Toast.makeText(context, "Edit profile page coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> removeProfilePicture()
        }
    }

    private fun removeProfilePicture() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val updateResult = FirebaseManager.updateUserProfileImage(uid, "") // Save an empty string
            if (updateResult.isSuccess) {
                binding.profileImageAdmin.setImageResource(R.drawable.ic_profile)
                Toast.makeText(context, "Profile picture removed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error removing photo: ${updateResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}