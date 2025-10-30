package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentOrderDetailBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {
    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val args: OrderDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup is now handled in onStart to ensure network check happens first
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            setupUI()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupUI() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        val order = args.order

        binding.orderIdDetail.text = "Order #${order.id.takeLast(6)}"
        binding.orderStatusDetail.text = "Status: ${order.status}"

        binding.orderItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.orderItemsRecyclerView.adapter = OrderDetailAdapter(order.items)

        if (order.status == "Pending Pickup" || order.status == "Ready for Pickup") {
            binding.cancelOrderButton.visibility = View.VISIBLE
        }

        binding.cancelOrderButton.setOnClickListener {
            showCancelConfirmationDialog()
        }

        // Stop shimmer and show content
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelOrder()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateProductOrderStatus(args.order.id, "Cancelled")
            if (result.isSuccess) {
                Toast.makeText(context, "Your order has been cancelled.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Go back to the previous screen
            } else {
                Toast.makeText(context, "Failed to cancel order. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
