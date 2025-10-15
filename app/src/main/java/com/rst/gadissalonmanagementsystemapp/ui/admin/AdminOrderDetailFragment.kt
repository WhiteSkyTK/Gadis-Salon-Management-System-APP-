package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminOrderDetailBinding
import com.rst.gadissalonmanagementsystemapp.ui.orders.OrderDetailAdapter

class AdminOrderDetailFragment : Fragment() {
    private var _binding: FragmentAdminOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val args: AdminOrderDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val order = args.order

        binding.orderIdDetail.text = "Order #${order.id.take(8)}"
        binding.customerNameDetail.text = "Customer: ${order.customerName}"
        binding.orderStatusDetail.text = "Status: ${order.status}"

        binding.orderItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.orderItemsRecyclerView.adapter = OrderDetailAdapter(order.items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
