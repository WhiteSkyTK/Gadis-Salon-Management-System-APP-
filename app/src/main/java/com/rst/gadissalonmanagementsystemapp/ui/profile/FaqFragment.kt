package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFaqBinding

class FaqFragment : Fragment() {
    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FaqAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForFaqUpdates()
    }

    private fun setupRecyclerView() {
        // Create the adapter once with an empty list
        adapter = FaqAdapter(emptyList())
        binding.faqRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.faqRecyclerView.adapter = adapter
    }

    private fun listenForFaqUpdates() {
        // Start listening for real-time updates to the FAQs in Firebase
        FirebaseManager.addFaqListener { faqList ->
            Log.d("FaqFragment", "Fetched ${faqList.size} FAQs from Firebase.")
            // When the data changes, update the adapter's list
            adapter.updateData(faqList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}