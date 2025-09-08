package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminProductsBinding

class AdminProductsFragment : Fragment() {
    private var _binding: FragmentAdminProductsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = AdminProductsPagerAdapter(this)
        binding.viewPagerProducts.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutProducts, binding.viewPagerProducts) { tab, position ->
            tab.text = when (position) {
                0 -> "Products"
                1 -> "Hairstyles"
                else -> null
            }
        }.attach()

        binding.fabAdd.setOnClickListener {
            when (binding.tabLayoutProducts.selectedTabPosition) {
                0 -> {
                    // "Products" tab is selected
                    // CORRECTED: Use the generated Directions class
                    val action = AdminProductsFragmentDirections.actionAdminProductsFragmentToAdminAddProductFragment()
                    findNavController().navigate(action)
                }
                1 -> {
                    // "Hairstyles" tab is selected
                    // CORRECTED: Use the generated Directions class
                    val action = AdminProductsFragmentDirections.actionAdminProductsFragmentToAdminAddHairstyleFragment()
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}