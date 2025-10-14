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
import com.rst.gadissalonmanagementsystemapp.ui.shop.HairstyleItemAdapter
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentItemGridBinding
import kotlinx.coroutines.launch

class HairstylesGridFragment : Fragment() {

    private var _binding: FragmentItemGridBinding? = null
    private val binding get() = _binding!!
    private val TAG = "HairstylesGridFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()

        // Launch a coroutine to fetch the hairstyles from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching hairstyles from Firebase...")
            val result = FirebaseManager.getAllHairstyles()

            // --- STOP SHIMMER & SHOW DATA ---
            if (isAdded) { // Ensure fragment is still attached
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.gridRecyclerView.visibility = View.VISIBLE
            }

            if (result.isSuccess) {
                val hairstyleList = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Successfully fetched ${hairstyleList.size} hairstyles.")
                binding.gridRecyclerView.adapter = HairstyleItemAdapter(hairstyleList) { hairstyle ->
                    findNavController().navigate(ShopFragmentDirections.actionShopFragmentToHairstyleDetailFragment(hairstyle))
                }
            } else {
                Toast.makeText(context, "Error fetching hairstyles.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}