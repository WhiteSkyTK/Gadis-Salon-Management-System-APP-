package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerInventoryBinding

class WorkerInventoryFragment : Fragment() {
    private var _binding: FragmentWorkerInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var inventoryAdapter: WorkerInventoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForInventoryUpdates()
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
        // Start listening for real-time updates from Firebase
        FirebaseManager.addProductsListener { productList ->
            Log.d("WorkerInventory", "Fetched ${productList.size} products from Firebase.")
            // When the data changes, update the adapter's list
            inventoryAdapter.updateData(productList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}