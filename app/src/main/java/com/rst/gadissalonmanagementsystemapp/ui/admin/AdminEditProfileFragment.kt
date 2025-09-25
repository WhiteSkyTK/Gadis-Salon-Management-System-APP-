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
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditProfileBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AdminEditProfileFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {
    private var _binding: FragmentAdminEditProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null
    private var currentUserData: User? = null

    // --- Activity Result Launchers for image picking and permissions ---
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takeImage() else Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.profileImageEdit.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.profileImageEdit.setImageURI(it)
            }
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentData()

        binding.profileImageEdit.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "EditProfilePic")
        }
        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadCurrentData() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getUser(uid)
            if (result.isSuccess) {
                currentUserData = result.getOrNull()
                binding.nameInput.setText(currentUserData?.name)
                binding.phoneInput.setText(currentUserData?.phone)
                binding.emailText.text = currentUserData?.email
                binding.profileImageEdit.load(currentUserData?.imageUrl) {
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            }
        }
    }

    private fun saveChanges() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val newName = binding.nameInput.text.toString().trim()
        val newPhone = binding.phoneInput.text.toString().trim()

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveChangesButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            var finalImageUrl = currentUserData?.imageUrl ?: ""
            // First, check if a new image was selected to be uploaded
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "profile_pictures", "$uid.jpg")
                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show()
                    binding.saveChangesButton.isEnabled = true
                    return@launch
                }
            }

            // Now, update the user's profile in Firestore
            val updateResult = FirebaseManager.updateUserProfile(uid, newName, newPhone, finalImageUrl)
            if (updateResult.isSuccess) {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${updateResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.saveChangesButton.isEnabled = true
            }
        }
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null
                binding.profileImageEdit.setImageResource(R.drawable.ic_profile)
                // We'll save an empty string to Firebase to signify "no image"
                viewLifecycleOwner.lifecycleScope.launch {
                    FirebaseManager.updateUserProfileImage(Firebase.auth.currentUser!!.uid, "")
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}