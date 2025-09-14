package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerOrdersBinding

class WorkerOrdersFragment : Fragment() {
    private var _binding: FragmentWorkerOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.workerOrdersRecyclerView.layoutManager = LinearLayoutManager(context)

        // Listen for real-time updates to pending product orders
        FirebaseManager.addPendingOrdersListener { orders ->
            binding.workerOrdersRecyclerView.adapter = WorkerOrdersAdapter(orders)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}