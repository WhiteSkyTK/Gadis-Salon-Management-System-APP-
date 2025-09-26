package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminSalesBinding

class AdminSalesFragment : Fragment() {
    private var _binding: FragmentAdminSalesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = AdminSalesPagerAdapter(this)
        binding.viewPagerSales.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutSales, binding.viewPagerSales) { tab, position ->
            tab.text = when (position) {
                0 -> "Service Bookings"
                1 -> "Product Orders"
                else -> null
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}