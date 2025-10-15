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
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class AdminSupportFragment : Fragment() {

    private var _binding: FragmentAdminSupportBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminSupportFragment"

    private lateinit var supportAdapter: AdminSupportAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.fabComposeMessage.setOnClickListener {
            findNavController().navigate(R.id.action_adminSupportFragment_to_adminComposeMessageFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            loadMessages()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        // Create the adapter ONCE with an empty list
        supportAdapter = AdminSupportAdapter(
            messages = emptyList(),
            onItemClick = { message ->
                val action = AdminSupportFragmentDirections.actionAdminSupportFragmentToAdminTicketDetailFragment(message)
                findNavController().navigate(action)
            },
            onStatusChange = { message, newStatus ->
                updateMessageStatus(message.id, newStatus)
            },
            onDelete = { message ->
                confirmAndDeleteMessage(message.id)
            }
        )
        binding.supportRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.supportRecyclerView.adapter = supportAdapter
    }

    private fun loadMessages() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.supportRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllSupportMessages()

            if (!isAdded) return@launch
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (result.isSuccess) {
                val messages = result.getOrNull() ?: emptyList()
                if (messages.isEmpty()) {
                    binding.supportRecyclerView.visibility = View.GONE
                    binding.emptyViewText.visibility = View.VISIBLE
                } else {
                    binding.supportRecyclerView.visibility = View.VISIBLE
                    binding.emptyViewText.visibility = View.GONE
                    supportAdapter.updateData(messages)
                }
            } else {
                val error = result.exceptionOrNull()?.message
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                binding.emptyViewText.visibility = View.VISIBLE
            }
        }
    }

    private fun updateMessageStatus(messageId: String, newStatus: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateSupportMessageStatus(messageId, newStatus)
            if (result.isSuccess) {
                Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                loadMessages() // Refresh the list
            } else {
                // Display the specific error message from Firebase
                val errorMessage = result.exceptionOrNull()?.message ?: "An unknown error occurred."
                Log.e(TAG, "Error updating status: $errorMessage")
            }
        }
    }

    private fun confirmAndDeleteMessage(messageId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to permanently delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = FirebaseManager.deleteSupportMessage(messageId)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        loadMessages() // Refresh the list
                    } else {
                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
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
