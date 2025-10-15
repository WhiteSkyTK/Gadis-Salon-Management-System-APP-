package com.rst.gadissalonmanagementsystemapp.ui.admin

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
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminListBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class AdminHairstylesListFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminHairstylesList"
    private lateinit var adapter: AdminHairstyleAdapter
    private var hairstylesListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for live updates when the screen is visible
        // Check for internet before fetching data
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForHairstyleUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop the listener when the screen is not visible to prevent memory leaks
        hairstylesListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = AdminHairstyleAdapter(
            items = emptyList(),
            onEditClick = { hairstyle ->
                // Navigate to the edit screen, passing the selected hairstyle
                val action = AdminProductsFragmentDirections.actionAdminProductsFragmentToAdminEditHairstyleFragment(hairstyle)
                findNavController().navigate(action)
            },
            onDeleteClick = { hairstyle ->
                // Call the FirebaseManager to delete the hairstyle
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = FirebaseManager.deleteHairstyle(hairstyle)
                    if (result.isSuccess) {
                        Toast.makeText(context, "${hairstyle.name} deleted.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error deleting hairstyle.", Toast.LENGTH_SHORT).show()
                    }
                    // The real-time listener will handle refreshing the UI automatically
                }
            }
        )
        binding.adminRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.adminRecyclerView.adapter = adapter
    }

    private fun listenForHairstyleUpdates() {
        // --- THIS IS THE UPDATED LOGIC ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.adminRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        hairstylesListener = FirebaseManager.addHairstylesListener { hairstyleList ->
            if (view == null) return@addHairstylesListener

            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (hairstyleList.isEmpty()) {
                binding.adminRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
            } else {
                binding.adminRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                adapter.updateData(hairstyleList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}