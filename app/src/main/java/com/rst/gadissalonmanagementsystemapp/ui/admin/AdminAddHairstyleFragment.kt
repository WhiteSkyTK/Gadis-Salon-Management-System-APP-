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
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddHairstyleBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AdminAddHairstyleFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminAddHairstyleBinding? = null
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
            binding.hairstyleImagePreview.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.hairstyleImagePreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddHairstyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch the stylists from Firebase and create the chips
        populateStylistChips()

        // Updated click listener to show the bottom sheet
        binding.hairstyleImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "HairstyleImagePicker")
        }

        binding.saveHairstyleButton.setOnClickListener {
            saveHairstyle()
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            populateStylistChips()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    // --- This function is called when an option is selected in the bottom sheet ---
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                binding.hairstyleImagePreview.setImageResource(R.drawable.ic_add_a_photo)
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

    private fun populateStylistChips() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllUsers()

            if (!isAdded) return@launch
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE

            if (result.isSuccess) {
                val stylists = (result.getOrNull() ?: emptyList()).filter { it.role == "WORKER" }
                binding.stylistChipGroup.removeAllViews()
                stylists.forEach { stylistUser ->
                    val chip = Chip(context).apply {
                        text = stylistUser.name; isCheckable = true; tag = stylistUser.id
                    }
                    binding.stylistChipGroup.addView(chip)
                }
            } else {
                Toast.makeText(context, "Error fetching stylists", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveHairstyle() {
        val name = binding.nameInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val price = binding.priceInput.text.toString().toDoubleOrNull()
        val duration = binding.durationInput.text.toString().toIntOrNull()
        val selectedStylistIds = binding.stylistChipGroup.checkedChipIds.mapNotNull { chipId ->
            view?.findViewById<Chip>(chipId)?.tag as? String
        }

        if (name.isEmpty() || price == null || duration == null || selectedImageUri == null || selectedStylistIds.isEmpty()) {
            Toast.makeText(context, "Please fill all fields, select an image, and choose at least one stylist", Toast.LENGTH_LONG).show()
            return
        }

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveHairstyleButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val hairstyleId = "hs_${UUID.randomUUID()}"
            // 1. Upload Image
            val imageUploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "hairstyles", "$hairstyleId.jpg")

            if (imageUploadResult.isSuccess) {
                val imageUrl = imageUploadResult.getOrNull().toString()

                // 2. Create Hairstyle Object
                val newHairstyle = Hairstyle(
                    id = hairstyleId,
                    name = name,
                    description = description,
                    price = price,
                    durationHours = duration,
                    availableStylistIds = selectedStylistIds,
                    imageUrl = imageUrl
                )

                // 3. Save Hairstyle to Firestore
                val addHairstyleResult = FirebaseManager.addHairstyle(newHairstyle)
                if (addHairstyleResult.isSuccess) {
                    // Make sure to hide loading and re-enable button in both success and failure cases
                    binding.loadingIndicator.visibility = View.GONE
                    binding.saveHairstyleButton.isEnabled = true
                    Toast.makeText(context, "$name added successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    // Make sure to hide loading and re-enable button in both success and failure cases
                    binding.loadingIndicator.visibility = View.GONE
                    binding.saveHairstyleButton.isEnabled = true
                    Toast.makeText(context, "Error saving hairstyle: ${addHairstyleResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                // Make sure to hide loading and re-enable button in both success and failure cases
                binding.loadingIndicator.visibility = View.GONE
                binding.saveHairstyleButton.isEnabled = true
                Toast.makeText(context, "Error uploading image: ${imageUploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            // Make sure to hide loading and re-enable button in both success and failure cases
            binding.loadingIndicator.visibility = View.GONE
            binding.saveHairstyleButton.isEnabled = true
            binding.saveHairstyleButton.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}