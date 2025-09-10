package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminSupportBinding
import kotlinx.coroutines.launch

class AdminSupportFragment : Fragment() {

    private var _binding: FragmentAdminSupportBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminSupportFragment"


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.supportRecyclerView.layoutManager = LinearLayoutManager(context)

        loadMessages()

        binding.fabComposeMessage.setOnClickListener {
            // Navigate to the new compose screen
            findNavController().navigate(R.id.action_adminSupportFragment_to_adminComposeMessageFragment)
        }
    }

    private fun loadMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllSupportMessages()
            if (result.isSuccess) {
                val messages = result.getOrNull() ?: emptyList()
                binding.supportRecyclerView.adapter = AdminSupportAdapter(
                    messages = messages,
                    onItemClick = { message ->
                        // Navigate to the reply screen when an item is clicked
                        val action = AdminSupportFragmentDirections.actionAdminSupportFragmentToAdminReplyMessageFragment(message)
                        findNavController().navigate(action)
                    },
                    onStatusChange = { message, newStatus ->
                        updateMessageStatus(message.id, newStatus)
                    },
                    onDelete = { message ->
                        confirmAndDeleteMessage(message.id)
                    }
                )
            } else {
                val error = result.exceptionOrNull()?.message
                Log.e(TAG, "Error fetching messages: $error")
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMessageStatus(messageId: String, newStatus: String) {
        lifecycleScope.launch {
            FirebaseManager.updateSupportMessageStatus(messageId, newStatus)
            Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
            loadMessages() // Refresh the list to show the change
        }
    }

    private fun confirmAndDeleteMessage(messageId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to permanently delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FirebaseManager.deleteSupportMessage(messageId)
                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    loadMessages() // Refresh the list
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
