package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerInventoryBinding

class WorkerInventoryFragment : Fragment() {

    private var _binding: FragmentWorkerInventoryBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Add logic here to fetch the product list from Firebase
        // and set up the RecyclerView adapter.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}