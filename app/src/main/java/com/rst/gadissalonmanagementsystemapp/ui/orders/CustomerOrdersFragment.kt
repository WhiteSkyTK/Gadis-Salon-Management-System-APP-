package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.os.Bundle
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
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCustomerOrdersBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class CustomerOrdersFragment : Fragment() {
    private var _binding: FragmentCustomerOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersAdapter: CustomerOrdersAdapter
    private var ordersListener: ListenerRegistration? = null
    private lateinit var offlineContainer: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offlineContainer = view.findViewById(R.id.offline_container)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false) // Hide offline screen
            listenForMyOrders()
        } else {
            showOfflineUI(true) // Show offline screen
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is not visible
        ordersListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        offlineContainer.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun listenForMyOrders() {
        // Start shimmer and set initial visibility
        binding.shimmerViewContainerOrders.startShimmer()
        binding.shimmerViewContainerOrders.visibility = View.VISIBLE
        binding.customerOrdersRecyclerView.visibility = View.GONE
        binding.emptyViewTextOrders.visibility = View.GONE

        ordersListener = FirebaseManager.addCurrentUserOrdersListener { myOrders ->
            if (view == null) return@addCurrentUserOrdersListener

            // Stop shimmer
            binding.shimmerViewContainerOrders.stopShimmer()
            binding.shimmerViewContainerOrders.visibility = View.GONE

            if (myOrders.isEmpty()) {
                // Show empty message if there are no orders
                binding.customerOrdersRecyclerView.visibility = View.GONE
                binding.emptyViewTextOrders.visibility = View.VISIBLE
            } else {
                // Show the list if there are orders
                binding.customerOrdersRecyclerView.visibility = View.VISIBLE
                binding.emptyViewTextOrders.visibility = View.GONE
                ordersAdapter.updateData(myOrders)
            }
        }
    }

    private fun setupRecyclerView() {
        ordersAdapter = CustomerOrdersAdapter(
            orders = emptyList(),
            onItemClick = { order ->
                val action = CustomerOrdersFragmentDirections.actionCustomerOrdersFragmentToOrderDetailFragment(order)
                findNavController().navigate(action)
            },
            onActionClick = { order, newStatus ->
                // This is what happens when the "I Have Arrived" button is clicked
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = FirebaseManager.updateProductOrderStatus(order.id, newStatus)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Thank you! Order completed.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update order status.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.customerOrdersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.customerOrdersRecyclerView.adapter = ordersAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}