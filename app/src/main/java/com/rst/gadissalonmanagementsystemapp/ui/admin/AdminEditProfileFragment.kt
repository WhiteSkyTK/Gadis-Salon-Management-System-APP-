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
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.io.File

class AdminEditProfileFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {
    private var _binding: FragmentAdminEditProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null
    private var currentUserData: User? = null
    private var imageRemoved = false

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
        binding.profileImageEdit.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "EditProfilePic")
        }
        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            loadCurrentData()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
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
                binding.profileImageEdit.load(currentUserData?.imageUrl?.ifEmpty { R.drawable.ic_profile }) {
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

        // --- VALIDATION LOGIC ---
        binding.nameLayout.error = null
        binding.phoneLayout.error = null
        var isValid = true
        if (newName.isEmpty()) {
            binding.nameLayout.error = "Name cannot be empty"
            isValid = false
        }
        if (newPhone.length != 10 || !newPhone.all { it.isDigit() }) {
            binding.phoneLayout.error = "Please enter a valid 10-digit phone number"
            isValid = false
        }
        if (!isValid) return
        // --- END VALIDATION LOGIC ---

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveChangesButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var finalImageUrl = currentUserData?.imageUrl ?: ""
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "profile_pictures", "$uid.jpg")
                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show()
                    binding.loadingIndicator.visibility = View.GONE
                    binding.saveChangesButton.isEnabled = true
                    return@launch
                }
            } else if (imageRemoved) {
                finalImageUrl = ""
            }

            val updateResult = FirebaseManager.updateUserProfile(uid, newName, newPhone, finalImageUrl)

            if (updateResult.isSuccess) {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${updateResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }

            binding.loadingIndicator.visibility = View.GONE
            binding.saveChangesButton.isEnabled = true
        }
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null
                imageRemoved = true
                binding.profileImageEdit.setImageResource(R.drawable.ic_profile)
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