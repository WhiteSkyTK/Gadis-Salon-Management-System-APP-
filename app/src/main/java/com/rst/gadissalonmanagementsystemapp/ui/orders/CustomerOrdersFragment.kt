package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCustomerOrdersBinding

class CustomerOrdersFragment : Fragment() {
    private var _binding: FragmentCustomerOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersAdapter: CustomerOrdersAdapter
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for orders when the screen is visible
        ordersListener = FirebaseManager.addCurrentUserOrdersListener { myOrders ->
            if (view != null) {
                ordersAdapter.updateData(myOrders)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is not visible
        ordersListener?.remove()
    }

    private fun setupRecyclerView() {
        ordersAdapter = CustomerOrdersAdapter(emptyList()) { order ->
            // This is the new click logic
            val action = CustomerOrdersFragmentDirections.actionCustomerOrdersFragmentToOrderDetailFragment(order)
            findNavController().navigate(action)
        }

        binding.customerOrdersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.customerOrdersRecyclerView.adapter = ordersAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}