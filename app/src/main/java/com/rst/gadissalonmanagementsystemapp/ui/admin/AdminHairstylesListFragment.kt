package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminListBinding

// This fragment displays a list of all hairstyles for the admin.
class AdminHairstylesListFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adminRecyclerView.layoutManager = LinearLayoutManager(context)

        // Observe the LiveData from AppData
        AppData.allHairstyles.observe(viewLifecycleOwner) { hairstyleList ->
            // Create and set the adapter when the data changes
            binding.adminRecyclerView.adapter = AdminHairstyleAdapter(hairstyleList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}