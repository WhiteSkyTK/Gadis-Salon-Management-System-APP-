package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.SupportMessage
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentContactBinding
import kotlinx.coroutines.launch
import java.util.UUID

class AdminComposeMessageFragment : Fragment() {
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill the admin's name and email
        val adminUser = Firebase.auth.currentUser
        binding.nameInput.setText(adminUser?.displayName ?: "Admin")
        binding.emailInput.setText(adminUser?.email ?: "")
        binding.emailLayout.hint = "Recipient Email" // Change hint for clarity

        binding.sendMessageButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val adminUser = Firebase.auth.currentUser
        val recipientEmail = binding.emailInput.text.toString().trim()
        val messageText = binding.messageInput.text.toString().trim()

        if (adminUser == null) return
        if (messageText.isEmpty() || recipientEmail.isEmpty()) {
            Toast.makeText(context, "Recipient email and message are required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.sendMessageButton.isEnabled = false

        // Create a message object from the admin
        val supportMessage = SupportMessage(
            id = UUID.randomUUID().toString(),
            senderUid = adminUser.uid,
            senderName = "Admin Support", // Or adminUser.displayName
            senderEmail = adminUser.email ?: "",
            message = messageText
            // In a real app, you might add a recipientId field here
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.sendSupportMessage(supportMessage)
            if (result.isSuccess) {
                Toast.makeText(context, "Message sent!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.sendMessageButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
