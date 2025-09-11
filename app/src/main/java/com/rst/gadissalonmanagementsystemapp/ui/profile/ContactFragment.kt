package com.rst.gadissalonmanagementsystemapp.ui.profile

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

class ContactFragment : Fragment() {
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = Firebase.auth.currentUser
        // Pre-fill the form with the logged-in user's details
        binding.nameInput.setText(currentUser?.displayName ?: "")
        binding.emailInput.setText(currentUser?.email ?: "")

        binding.sendMessageButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val currentUser = Firebase.auth.currentUser
        val messageText = binding.messageInput.text.toString().trim()

        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to send a message.", Toast.LENGTH_SHORT).show()
            return
        }
        if (messageText.isEmpty()) {
            binding.messageLayout.error = "Message cannot be empty"
            return
        }

        binding.sendMessageButton.isEnabled = false
        // You can show a loading indicator here

        val supportMessage = SupportMessage(
            senderUid = currentUser.uid,
            senderName = binding.nameInput.text.toString(),
            senderEmail = binding.emailInput.text.toString(),
            message = messageText
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.sendSupportMessage(supportMessage)
            if (result.isSuccess) {
                Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_SHORT).show()
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