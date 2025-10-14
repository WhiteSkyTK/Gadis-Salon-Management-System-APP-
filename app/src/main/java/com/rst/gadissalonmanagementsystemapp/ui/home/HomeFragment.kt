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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.ui.shop.HairstyleItemAdapter
import com.rst.gadissalonmanagementsystemapp.ui.home.HomeItemAdapter
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

        setupRecyclerViews()
        setupClickListeners()
        loadDataFromFirebase()
    }

    private fun setupRecyclerViews() {
        // Use a GridLayoutManager with 2 columns
        binding.recyclerViewProducts.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerViewHairstyles.layoutManager = GridLayoutManager(context, 2)
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
        binding.shimmerViewProducts.startShimmer()
        binding.shimmerViewHairstyles.startShimmer()

        viewLifecycleOwner.lifecycleScope.launch {
            // --- Fetch Products ---
            val productsResult = FirebaseManager.getAllProducts()
            if (!isAdded) return@launch

            binding.shimmerViewProducts.stopShimmer()
            binding.shimmerViewProducts.visibility = View.GONE
            binding.recyclerViewProducts.visibility = View.VISIBLE

            if (productsResult.isSuccess) {
                val productList = productsResult.getOrNull()?.take(4) ?: emptyList()
                Log.d(TAG, "Successfully fetched ${productList.size} products.")
                binding.recyclerViewProducts.adapter = HomeItemAdapter(productList) { product ->
                    val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product, "CUSTOMER")
                    findNavController().navigate(action)
                }
            } else {
                val error = productsResult.exceptionOrNull()?.message
                Log.e(TAG, "Error fetching products: $error")
                Toast.makeText(context, "Error fetching products", Toast.LENGTH_SHORT).show()
            }

            // --- Fetch Hairstyles ---
            val hairstylesResult = FirebaseManager.getAllHairstyles()
            if (!isAdded) return@launch

            // --- STOP SHIMMER & SHOW DATA ---
            binding.shimmerViewHairstyles.stopShimmer()
            binding.shimmerViewHairstyles.visibility = View.GONE
            binding.recyclerViewHairstyles.visibility = View.VISIBLE

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
