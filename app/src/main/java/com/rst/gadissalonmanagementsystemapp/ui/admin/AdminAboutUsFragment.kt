package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAboutUsEditBinding // We'll create this layout next
import kotlinx.coroutines.launch

class AdminAboutUsFragment : Fragment() {

    private var _binding: FragmentAdminAboutUsEditBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAboutUsEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadContent()

        binding.saveButton.setOnClickListener {
            saveContent()
        }
    }

    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAboutUsContent()
            if (result.isSuccess) {
                val content = result.getOrNull()
                binding.salonAboutInput.setText(content?.get("salon_about"))
                binding.appAboutInput.setText(content?.get("app_about"))
            }
        }
    }

    private fun saveContent() {
        val salonAboutText = binding.salonAboutInput.text.toString()
        val appAboutText = binding.appAboutInput.text.toString()

        binding.saveButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateAboutUsContent(salonAboutText, appAboutText)
            if (result.isSuccess) {
                Toast.makeText(context, "Content updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            binding.saveButton.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}