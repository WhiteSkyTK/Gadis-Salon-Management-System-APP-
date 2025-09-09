package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminUsersPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = AdminUserListFragment()
        // We pass the role to the fragment so it knows which users to display
        fragment.arguments = Bundle().apply {
            putString("USER_ROLE", when (position) {
                0 -> "CUSTOMER"
                1 -> "WORKER"
                2 -> "ADMIN"
                else -> "CUSTOMER"
            })
        }
        return fragment
    }
}