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
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditHairstyleBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import java.io.File

class AdminEditHairstyleFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminEditHairstyleBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditHairstyleFragmentArgs by navArgs()
    private lateinit var hairstyleToEdit: Hairstyle
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    // --- Activity Result Launchers ---
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takeImage() else Toast.makeText(context, "Camera permission needed.", Toast.LENGTH_SHORT).show()
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.hairstyleImagePreview.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.hairstyleImagePreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditHairstyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hairstyleToEdit = args.hairstyle
        populateFields()
        binding.hairstyleImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "HairstyleImagePicker")
        }
        binding.saveChangesButton.setOnClickListener { saveChanges() }
    }


    private fun populateFields() {
        binding.hairstyleImagePreview.load(hairstyleToEdit.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
        }
        binding.nameInput.setText(hairstyleToEdit.name)
        binding.descriptionInput.setText(hairstyleToEdit.description)
        binding.priceInput.setText(hairstyleToEdit.price.toString())
        binding.durationInput.setText(hairstyleToEdit.durationHours.toString())

        // Fetch all workers and pre-select the ones assigned to this hairstyle
        populateAndSelectStylistChips()
    }

    private fun populateAndSelectStylistChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllUsers()
            if (result.isSuccess) {
                val stylists = (result.getOrNull() ?: emptyList()).filter { it.role == "WORKER" }
                binding.stylistChipGroup.removeAllViews()
                stylists.forEach { stylist ->
                    val chip = Chip(context).apply {
                        text = stylist.name
                        isCheckable = true
                        tag = stylist.id
                        isChecked = hairstyleToEdit.availableStylistIds.contains(stylist.id)
                    }
                    binding.stylistChipGroup.addView(chip)
                }
            }
        }
    }

    private fun saveChanges() {
        val newName = binding.nameInput.text.toString().trim()
        val newDescription = binding.descriptionInput.text.toString().trim()
        val newPrice = binding.priceInput.text.toString().toDoubleOrNull()
        val newDuration = binding.durationInput.text.toString().toIntOrNull()
        val selectedStylistIds = binding.stylistChipGroup.checkedChipIds.mapNotNull { view?.findViewById<Chip>(it)?.tag as? String }

        if (newName.isEmpty() || newPrice == null || newDuration == null || selectedStylistIds.isEmpty()) {
            Toast.makeText(context, "Please fill all fields and select at least one stylist.", Toast.LENGTH_LONG).show()
            return
        }

        binding.saveChangesButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var imageUrl = hairstyleToEdit.imageUrl
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "hairstyles", hairstyleToEdit.id)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Image upload failed.", Toast.LENGTH_SHORT).show()
                    binding.saveChangesButton.isEnabled = true
                    return@launch
                }
            }

            val updatedHairstyle = hairstyleToEdit.copy(
                name = newName,
                description = newDescription,
                price = newPrice,
                durationHours = newDuration,
                availableStylistIds = selectedStylistIds,
                imageUrl = imageUrl
            )

            val result = FirebaseManager.updateHairstyle(updatedHairstyle)
            if (result.isSuccess) {
                Toast.makeText(context, "Hairstyle updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            binding.saveChangesButton.isEnabled = true
        }
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null
                binding.hairstyleImagePreview.setImageResource(R.drawable.ic_add_a_photo)
                hairstyleToEdit = hairstyleToEdit.copy(imageUrl = "")
            }
        }
    }

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