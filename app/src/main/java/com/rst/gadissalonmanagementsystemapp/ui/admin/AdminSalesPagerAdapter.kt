package com.rst.gadissalonmanagementsystemapp.ui.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminSalesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminBookingsFragment() // Your existing bookings list
            1 -> AdminOrdersFragment()   // The new orders list we will create
            else -> throw IllegalStateException("Invalid position")
        }
    }
}