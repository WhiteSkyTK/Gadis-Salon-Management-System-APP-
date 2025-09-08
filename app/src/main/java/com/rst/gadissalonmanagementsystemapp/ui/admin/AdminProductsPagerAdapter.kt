package com.rst.gadissalonmanagementsystemapp.ui.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminProductsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminProductsListFragment()
            1 -> AdminHairstylesListFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}