package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerOrdersBinding
import kotlinx.coroutines.launch

class WorkerOrdersFragment : Fragment() {
    private var _binding: FragmentWorkerOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersAdapter: WorkerOrdersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForPendingOrders()
    }

    private fun setupRecyclerView() {
        // Create the adapter once with an empty list
        ordersAdapter = WorkerOrdersAdapter(
            orders = mutableListOf(),
            // --- THIS IS THE FIX: Provide the click logic ---
            onMarkAsReady = { order ->
                markOrderAsReady(order.id)
            }
        )
        binding.workerOrdersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerOrdersRecyclerView.adapter = ordersAdapter
    }

    private fun listenForPendingOrders() {
        // Listen for real-time updates to pending product orders
        FirebaseManager.addPendingOrdersListener { orders ->
            Log.d("WorkerOrdersFragment", "Live update: Found ${orders.size} pending orders.")
            // Update the adapter's data
            (binding.workerOrdersRecyclerView.adapter as? WorkerOrdersAdapter)?.updateData(orders)
        }
    }

    private fun markOrderAsReady(orderId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateProductOrderStatus(orderId, "Ready for Pickup")
            if (result.isSuccess) {
                Toast.makeText(context, "Order marked as ready!", Toast.LENGTH_SHORT).show()
                // The real-time listener will automatically remove the item from the list
            } else {
                Toast.makeText(context, "Failed to update order.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}