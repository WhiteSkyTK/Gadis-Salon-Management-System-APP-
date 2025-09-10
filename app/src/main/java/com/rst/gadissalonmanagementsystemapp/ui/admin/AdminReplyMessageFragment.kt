package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminReplyMessageBinding

class AdminReplyMessageFragment : Fragment() {
    private var _binding: FragmentAdminReplyMessageBinding? = null
    private val binding get() = _binding!!
    private val args: AdminReplyMessageFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReplyMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = args.supportMessage

        // Display the original message details
        binding.senderInfoText.text = "From: ${message.senderName} (${message.senderEmail})"
        binding.originalMessageText.text = message.message

        binding.sendReplyButton.setOnClickListener {
            // TODO: In the future, this would send an email or an in-app reply
            Toast.makeText(context, "Reply feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}