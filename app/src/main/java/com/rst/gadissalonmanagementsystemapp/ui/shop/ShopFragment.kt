package com.rst.gadissalonmanagementsystemapp.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentShopBinding

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the adapter for the ViewPager
        val pagerAdapter = ShopViewPagerAdapter(this)
        binding.viewPagerShop.adapter = pagerAdapter

        // Link the TabLayout and the ViewPager2 together
        TabLayoutMediator(binding.tabLayout, binding.viewPagerShop) { tab, position ->
            tab.text = when (position) {
                0 -> "Products"
                1 -> "Hairstyles"
                else -> null
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}