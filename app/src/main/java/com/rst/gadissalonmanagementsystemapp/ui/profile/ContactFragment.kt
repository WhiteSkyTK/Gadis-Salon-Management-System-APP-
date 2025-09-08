package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentContactBinding

class ContactFragment : Fragment() {
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill user's info if they are logged in
        AppData.getCurrentUser()?.let {
            binding.nameInput.setText(it.name)
            binding.emailInput.setText(it.email)
        }

        binding.sendMessageButton.setOnClickListener {
            if (binding.messageInput.text.toString().isNotBlank()) {
                // In a real app, you would send this message to your backend
                Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.messageLayout.error = "Message cannot be empty"
            }
        }
    }
}