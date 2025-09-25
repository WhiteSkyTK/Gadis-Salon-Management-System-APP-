package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import kotlinx.coroutines.launch

class AdminEditHairstyleFragment : Fragment() {

    private var _binding: FragmentAdminEditHairstyleBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditHairstyleFragmentArgs by navArgs()
    private lateinit var hairstyleToEdit: Hairstyle

    // Note: This screen reuses the image picking logic, but for simplicity,
    // we are not including it here. You can add it back just like in the other fragments.

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditHairstyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hairstyleToEdit = args.hairstyle

        populateFields()

        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
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
                val allUsers = result.getOrNull() ?: emptyList()
                val stylists = allUsers.filter { it.role.equals("WORKER", ignoreCase = true) }

                binding.stylistChipGroup.removeAllViews()
                stylists.forEach { stylist ->
                    val chip = Chip(context)
                    chip.text = stylist.name
                    chip.isCheckable = true
                    chip.tag = stylist.id
                    // Check the chip if this stylist is already assigned
                    if (hairstyleToEdit.availableStylistIds.contains(stylist.id)) {
                        chip.isChecked = true
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
        val selectedStylistIds = binding.stylistChipGroup.checkedChipIds.mapNotNull { chipId ->
            view?.findViewById<Chip>(chipId)?.tag as? String
        }

        if (newName.isEmpty() || newPrice == null || newDuration == null || selectedStylistIds.isEmpty()) {
            Toast.makeText(context, "Please fill all fields and select at least one stylist.", Toast.LENGTH_LONG).show()
            return
        }

        // Create the updated hairstyle object, keeping the original ID and image URL
        val updatedHairstyle = hairstyleToEdit.copy(
            name = newName,
            description = newDescription,
            price = newPrice,
            durationHours = newDuration,
            availableStylistIds = selectedStylistIds
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateHairstyle(updatedHairstyle)
            if (result.isSuccess) {
                Toast.makeText(context, "Hairstyle updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}