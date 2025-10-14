package com.rst.gadissalonmanagementsystemapp.ui.shop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.ui.home.HomeItemAdapter
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentItemGridBinding
import kotlinx.coroutines.launch

class ProductsGridFragment : Fragment() {

    private var _binding: FragmentItemGridBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ProductsGridFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()

        // Launch a coroutine to fetch the products from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching products from Firebase...")
            val result = FirebaseManager.getAllProducts()

            // --- STOP SHIMMER & SHOW DATA ---
            if (isAdded) { // Ensure fragment is still attached
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.gridRecyclerView.visibility = View.VISIBLE
            }

            if (result.isSuccess) {
                val productList = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Successfully fetched ${productList.size} products.")
                val adapter = HomeItemAdapter(productList) { clickedProduct ->
                    val action = ShopFragmentDirections.actionShopFragmentToProductDetailFragment(clickedProduct)
                    findNavController().navigate(action)
                }
                binding.gridRecyclerView.adapter = adapter
            } else {
                Toast.makeText(context, "Error fetching products.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}