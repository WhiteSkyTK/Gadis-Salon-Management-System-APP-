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
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProfileBinding
import java.io.File

class ProfileFragment : Fragment(),  ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var latestTmpUri: Uri? = null

    // --- Launcher for CAMERA PERMISSION request ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Now we can launch the camera.
            takeImage()
        } else {
            // User denied the permission. Show a message.
            Toast.makeText(context, "Camera permission is required to take a photo.", Toast.LENGTH_LONG).show()
        }
    }
    // --- Activity Result Launchers for Gallery and Camera ---
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.profileImage.setImageURI(it)
            AppData.getCurrentUser()?.profileImageUri = it.toString()
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                binding.profileImage.setImageURI(it)
                AppData.getCurrentUser()?.profileImageUri = it.toString()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Get the currently logged-in user from AppData ---
        val currentUser = AppData.getCurrentUser()

        // --- 2. Display the user's information ---
        if (currentUser != null) {
            // If a user is logged in, display their details
            binding.userName.text = currentUser.name
            binding.userPhone.text = currentUser.phone
            // Load saved profile image if it exists
            currentUser.profileImageUri?.let {
                binding.profileImage.setImageURI(Uri.parse(it))
            }
        } else {
            // If for some reason no one is logged in, show default text
            binding.userName.text = "Guest User"
            binding.userPhone.text = "Please log in"
        }

        // --- Setup Click Listeners ---
        binding.contactSupportOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_contactFragment)
        }
        binding.settingsOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.locationOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_locationFragment)
        }
        binding.aboutUsOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutUsFragment)
        }

        binding.profileImage.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "ProfilePictureBottomSheet")
        }

        // --- 3. Get App Version Programmatically (this logic is the same) ---
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            binding.versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- This function is called from the Bottom Sheet ---
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto() //
            "remove" -> {
                binding.profileImage.setImageResource(R.drawable.ic_profile)
                AppData.getCurrentUser()?.profileImageUri = null
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            // Check if the permission is already granted
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                takeImage()
            }
            // TODO: You can add a check for shouldShowRequestPermissionRationale here if needed
            else -> {
                // Directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takeImage() {
        lifecycle.let {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takeImageLauncher.launch(uri)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireContext(), "${requireActivity().packageName}.provider", tmpFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}