package com.rst.gadissalonmanagementsystemapp.ui.profile

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCustomerEditProfileBinding
import kotlinx.coroutines.launch
import java.io.File

// Implement the listener interface from the Bottom Sheet
class CustomerEditProfileFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {
    private var _binding: FragmentCustomerEditProfileBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    // --- Activity Result Launchers for image picking and permissions ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) takeImage() else Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImageEdit.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.profileImageEdit.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentData()

        // --- ADDED: Click listener for the profile image ---
        binding.profileImageEdit.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "CustomerEditProfilePic")
        }

        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadCurrentData() {
        // Observe the user data from the ViewModel
        mainViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.nameInput.setText(user.name)
                binding.phoneInput.setText(user.phone)
                binding.profileImageEdit.load(user.imageUrl) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            var finalImageUrl = mainViewModel.currentUser.value?.imageUrl ?: ""

            // First, check if a new image was selected to be uploaded
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "profile_pictures", "$uid.jpg")
                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            // Now, update the user's profile in Firestore
            val result = FirebaseManager.updateUserProfile(uid, newName, newPhone, finalImageUrl)
            if (result.isSuccess) {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Update failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- This function is called from the Bottom Sheet ---
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null // Mark that we want to remove the image
                binding.profileImageEdit.setImageResource(R.drawable.ic_profile)
                // The actual removal (saving an empty string) will happen when "Save Changes" is clicked
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