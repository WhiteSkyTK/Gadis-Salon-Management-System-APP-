package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddHairstyleBinding
import java.util.UUID

class AdminAddHairstyleFragment : Fragment() {

    private var _binding: FragmentAdminAddHairstyleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddHairstyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateStylistChips()

        binding.saveHairstyleButton.setOnClickListener {
            saveHairstyle()
        }
    }

    private fun populateStylistChips() {
        AppData.allStylists.forEach { stylist ->
            val chip = Chip(context)
            chip.text = stylist.name
            chip.isCheckable = true
            chip.tag = stylist.id // Store the stylist's ID in the chip's tag
            binding.stylistChipGroup.addView(chip)
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

        // Validation
        if (name.isEmpty() || description.isEmpty() || price == null || duration == null || selectedStylistIds.isEmpty()) {
            Toast.makeText(context, "Please fill all fields and select at least one stylist", Toast.LENGTH_SHORT).show()
            return
        }

        val newHairstyle = Hairstyle(
            id = "hs_${UUID.randomUUID()}",
            name = name,
            description = description,
            price = price,
            durationHours = duration,
            availableStylistIds = selectedStylistIds
        )

        AppData.addHairstyle(newHairstyle)
        Toast.makeText(context, "$name added successfully", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}