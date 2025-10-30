package com.rst.gadissalonmanagementsystemapp.ui.worker

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
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerOrdersBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class WorkerOrdersFragment : Fragment() {
    private var _binding: FragmentWorkerOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersAdapter: WorkerOrdersAdapter
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for real-time updates when the screen is visible
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForWorkerOrders()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is not visible to prevent crashes
        ordersListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        ordersAdapter = WorkerOrdersAdapter(
            orders = mutableListOf(),
            onItemClick = { order ->
                // This is what happens when a worker clicks on an order
                val action = WorkerOrdersFragmentDirections.actionWorkerOrdersFragmentToWorkerOrderDetailFragment(order)
                findNavController().navigate(action)
            },
            onMarkAsReady = { order ->
                markOrderAsReady(order.id)
            }
        )
        binding.workerOrdersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerOrdersRecyclerView.adapter = ordersAdapter
    }


    private fun listenForWorkerOrders() {
        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.workerOrdersRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        // CHANGED: Called new FirebaseManager function
        ordersListener = FirebaseManager.addWorkerOrdersListener { orders ->
            if (view == null) return@addWorkerOrdersListener

            // --- STOP SHIMMER ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            // CHANGED: Updated log message
            Log.d("WorkerOrdersFragment", "Live update: Found ${orders.size} pending or ready orders.")

            if (orders.isEmpty()) {
                // Show empty message
                binding.workerOrdersRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
            } else {
                // Show the list
                binding.workerOrdersRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                ordersAdapter.updateData(orders)
            }
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