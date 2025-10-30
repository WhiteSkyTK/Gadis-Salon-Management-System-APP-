package com.rst.gadissalonmanagementsystemapp.ui.worker

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
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerOrderDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.orders.OrderDetailAdapter
import kotlinx.coroutines.launch

class WorkerOrderDetailFragment : Fragment() {
    private var _binding: FragmentWorkerOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val args: WorkerOrderDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val order = args.order

        binding.orderIdDetail.text = "Order #${order.id.takeLast(6)}"
        binding.customerNameDetail.text = "For: ${order.customerName}"
        binding.orderStatusDetail.text = "Status: ${order.status}"

        binding.orderItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.orderItemsRecyclerView.adapter = OrderDetailAdapter(order.items)

        if (order.status == "Ready for Pickup") {
            binding.completeOrderButton.visibility = View.VISIBLE
        } else {
            binding.completeOrderButton.visibility = View.GONE
        }

        binding.completeOrderButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = FirebaseManager.updateProductOrderStatus(order.id, "Completed")
                if (result.isSuccess) {
                    Toast.makeText(context, "Order marked as completed!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(context, "Failed to update status.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}