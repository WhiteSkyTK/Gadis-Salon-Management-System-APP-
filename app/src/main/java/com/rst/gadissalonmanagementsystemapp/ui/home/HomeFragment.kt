package com.rst.gadissalonmanagementsystemapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.HairstyleItemAdapter
import com.rst.gadissalonmanagementsystemapp.HomeItemAdapter
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val TAG = "HomeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadDataFromFirebase()
    }

    private fun setupClickListeners() {
        binding.viewAllProducts.setOnClickListener {
            // Navigate to the Shop and tell it to open the first tab (index 0)
            val action = HomeFragmentDirections.actionHomeFragmentToShopFragment(0)
            findNavController().navigate(action)
        }
        binding.viewAllHairstyles.setOnClickListener {
            // Navigate to the Shop and tell it to open the second tab (index 1)
            val action = HomeFragmentDirections.actionHomeFragmentToShopFragment(1)
            findNavController().navigate(action)
        }
    }

    private fun loadDataFromFirebase() {
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewHairstyles.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        viewLifecycleOwner.lifecycleScope.launch {
            // --- Fetch Products ---
            val productsResult = FirebaseManager.getAllProducts()
            if (productsResult.isSuccess) {
                val productList = productsResult.getOrNull()?.take(4) ?: emptyList()
                Log.d(TAG, "Successfully fetched ${productList.size} products.")
                binding.recyclerViewProducts.adapter = HomeItemAdapter(productList) { product ->
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product))
                }
            } else {
                val error = productsResult.exceptionOrNull()?.message
                Log.e(TAG, "Error fetching products: $error")
                Toast.makeText(context, "Error fetching products", Toast.LENGTH_SHORT).show()
            }

            // --- Fetch Hairstyles ---
            val hairstylesResult = FirebaseManager.getAllHairstyles()
            if (hairstylesResult.isSuccess) {
                val hairstyleList = hairstylesResult.getOrNull()?.take(4) ?: emptyList()
                Log.d(TAG, "Successfully fetched ${hairstyleList.size} hairstyles.")
                binding.recyclerViewHairstyles.adapter = HairstyleItemAdapter(hairstyleList) { hairstyle ->
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHairstyleDetailFragment(hairstyle))
                }
            } else {
                val error = hairstylesResult.exceptionOrNull()?.message
                Log.e(TAG, "Error fetching hairstyles: $error")
                Toast.makeText(context, "Error fetching hairstyles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
