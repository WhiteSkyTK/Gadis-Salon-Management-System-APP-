package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminListBinding

class AdminOrdersFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminOrdersAdapter
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for live order updates
        ordersListener = FirebaseManager.addAllProductOrdersListener { orderList ->
            if (view != null) {
                adapter.updateData(orderList)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop the listener to prevent memory leaks
        ordersListener?.remove()
    }

    private fun setupRecyclerView() {
        adapter = AdminOrdersAdapter(emptyList())
        binding.adminRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.adminRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
