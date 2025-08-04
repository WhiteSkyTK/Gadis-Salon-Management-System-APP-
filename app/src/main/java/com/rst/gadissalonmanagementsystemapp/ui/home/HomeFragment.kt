package com.rst.gadissalonmanagementsystemapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.HomeItemAdapter
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHomeBinding // Import ViewBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Handle "View All" Clicks ---
        binding.viewAllProducts.setOnClickListener {
            findNavController().navigate(R.id.shopFragment)
        }
        binding.viewAllHairstyles.setOnClickListener {
            findNavController().navigate(R.id.shopFragment)
        }

        // --- Setup Dummy Data for RecyclerViews ---
        val dummyProducts = listOf(
            Product("Eco Style Gel", "R50"),
            Product("Shampoo", "R80"),
            Product("Conditioner", "R85")
        )
        // Create an instance of our new adapter
        val productAdapter = HomeItemAdapter(dummyProducts)
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        // Set the adapter on the RecyclerView
        binding.recyclerViewProducts.adapter = productAdapter

        val dummyHairstyles = listOf(
            Product("Butterfly Locs", "R450"),
            Product("Dreadlocks", "R400"),
            Product("Cornrows", "R250")
        )
        // Create another instance of our reusable adapter
        val hairstyleAdapter = HomeItemAdapter(dummyHairstyles)
        binding.recyclerViewHairstyles.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        // Set the adapter on the RecyclerView
        binding.recyclerViewHairstyles.adapter = hairstyleAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}