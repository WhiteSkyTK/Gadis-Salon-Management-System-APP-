package com.rst.gadissalonmanagementsystemapp.ui.shop

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ShopViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance for each position
        return when (position) {
            0 -> ProductsGridFragment()
            1 -> HairstylesGridFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}