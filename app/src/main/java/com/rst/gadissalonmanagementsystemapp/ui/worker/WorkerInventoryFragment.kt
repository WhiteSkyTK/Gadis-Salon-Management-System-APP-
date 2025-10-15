package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerInventoryBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils

class WorkerInventoryFragment : Fragment() {
    private var _binding: FragmentWorkerInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var inventoryAdapter: WorkerInventoryAdapter
    private var productsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Check for internet before fetching data
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForInventoryUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        productsListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        // Create the adapter once with an empty list
        inventoryAdapter = WorkerInventoryAdapter(emptyList()) { product ->
            // This is what happens when a worker clicks an item
            val action = WorkerInventoryFragmentDirections.actionWorkerInventoryFragmentToProductDetailFragment(product)
            findNavController().navigate(action)
        }
        binding.inventoryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.inventoryRecyclerView.adapter = inventoryAdapter
    }

    private fun listenForInventoryUpdates() {
        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.inventoryRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        productsListener = FirebaseManager.addProductsListener { productList ->
            if (view == null) return@addProductsListener

            // --- STOP SHIMMER ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            Log.d("WorkerInventory", "Fetched ${productList.size} products from Firebase.")

            if (productList.isEmpty()) {
                // Show empty message
                binding.inventoryRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
            } else {
                // Show the list
                binding.inventoryRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                inventoryAdapter.updateData(productList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}