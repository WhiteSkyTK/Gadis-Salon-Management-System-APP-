package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import com.rst.gadissalonmanagementsystemapp.AdminMainActivity
import com.rst.gadissalonmanagementsystemapp.ProductOrder
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminOrderDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.orders.OrderDetailAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminOrderDetailFragment : Fragment() {
    private var _binding: FragmentAdminOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val args: AdminOrderDetailFragmentArgs by navArgs()
    private val db = Firebase.firestore
    private val functions = Firebase.functions
    private val TAG = "AdminOrderDetail"
    private lateinit var order: ProductOrder // Store the order

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminOrderDetailBinding.inflate(inflater, container, false)
        // Set the title in the main activity
        (activity as? AdminMainActivity)?.updateTitle("Order Details")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        order = args.order // Get the order from arguments

        // Set up the static info
        binding.orderIdDetail.text = "Order #${order.id.takeLast(6)}"
        binding.customerNameDetail.text = "Customer: ${order.customerName}"
        // binding.headerCard.orderStatusDetail.text = "Status: ${order.status}" // We use the spinner now

        // Set up the RecyclerView
        binding.orderItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.orderItemsRecyclerView.adapter = OrderDetailAdapter(order.items)

        // Set up the new edit/delete UI
        setupStatusSpinner()
        setupClickListeners()
    }

    private fun setupStatusSpinner() {
        val statuses = listOf("Pending Pickup", "Ready for Pickup", "Completed", "Cancelled", "Abandoned", "Processing", "Shipped", "Delivered")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.orderStatusSpinner.adapter = adapter

        // Set the spinner to the order's current status
        val currentStatusIndex = statuses.indexOf(order.status)
        if (currentStatusIndex != -1) {
            binding.orderStatusSpinner.setSelection(currentStatusIndex)
        }
    }

    private fun setupClickListeners() {
        // Save Status Button
        binding.saveStatusButton.setOnClickListener {
            val newStatus = binding.orderStatusSpinner.selectedItem.toString()
            saveStatus(newStatus)
        }

        // Delete Order Button
        binding.deleteOrderButton.setOnClickListener {
            deleteOrder()
        }
    }

    private fun saveStatus(newStatus: String) {
        // Disable button to prevent double-clicks
        binding.saveStatusButton.isEnabled = false
        binding.saveStatusButton.text = "Saving..."

        lifecycleScope.launch {
            try {
                // If moving to "Completed", call the Cloud Function
                if (newStatus == "Completed" && order.status != "Completed") {
                    val markOrderPaid = functions.getHttpsCallable("markOrderAsPaid")
                    markOrderPaid.call(mapOf("orderId" to order.id)).await()
                    Toast.makeText(context, "Order marked as paid!", Toast.LENGTH_SHORT).show()
                } else {
                    // For any other status change, just update the document
                    db.collection("product_orders").document(order.id)
                        .update("status", newStatus)
                        .await()
                    Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                }
                order.status = newStatus // Update local copy
                findNavController().popBackStack() // Go back after saving
            } catch (e: Exception) {
                Log.e(TAG, "Error saving status", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Re-enable button
                binding.saveStatusButton.isEnabled = true
                binding.saveStatusButton.text = "Save Status"
            }
        }
    }

    private fun deleteOrder() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to permanently delete this order? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("product_orders").document(order.id).delete().await()
                        Toast.makeText(context, "Order deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack() // Go back after deleting
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting order", e)
                        Toast.makeText(context, "Failed to delete order", Toast.LENGTH_SHORT).show()
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
