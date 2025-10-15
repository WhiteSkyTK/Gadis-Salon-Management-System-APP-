package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminGenericListBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils

class AdminOrdersFragment : Fragment() {
    private var _binding: FragmentAdminGenericListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminOrderAdapter
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminGenericListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        // We now inflate a NEW placeholder inside the loop on each iteration.
        val inflater = LayoutInflater.from(context)
        binding.placeholderContainer.removeAllViews() // Clear any old placeholders
        repeat(5) {
            val placeholder = inflater.inflate(R.layout.item_admin_order_placeholder, binding.placeholderContainer, false)
            binding.placeholderContainer.addView(placeholder)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForOrderUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        ordersListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = AdminOrderAdapter(emptyList()) { order ->
            val action = AdminSalesFragmentDirections.actionAdminSalesFragmentToAdminOrderDetailFragment(order)
            findNavController().navigate(action)
        }
        binding.listRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.listRecyclerView.adapter = adapter
    }

    private fun listenForOrderUpdates() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.listRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        ordersListener = FirebaseManager.addAllProductOrdersListener { orderList ->
            if (view == null) return@addAllProductOrdersListener

            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (orderList.isEmpty()) {
                binding.listRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
                binding.emptyViewText.text = "No product orders found."
            } else {
                binding.listRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                adapter.updateData(orderList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
