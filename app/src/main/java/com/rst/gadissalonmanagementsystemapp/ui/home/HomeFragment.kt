package com.rst.gadissalonmanagementsystemapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.HairstyleItemAdapter
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

        AppData.allProducts.observe(viewLifecycleOwner) { productList ->
            val productAdapter = HomeItemAdapter(productList) { product ->
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product))
            }
            binding.recyclerViewProducts.adapter = productAdapter
        }

        AppData.allHairstyles.observe(viewLifecycleOwner) { hairstyleList ->
            val hairstyleAdapter = HairstyleItemAdapter(hairstyleList) { hairstyle ->
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHairstyleDetailFragment(hairstyle))
            }
            binding.recyclerViewHairstyles.adapter = hairstyleAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}