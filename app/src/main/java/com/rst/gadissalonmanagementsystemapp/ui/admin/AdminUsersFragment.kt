package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminUsersBinding

class AdminUsersFragment : Fragment() {
    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = AdminUsersPagerAdapter(this)
        binding.viewPagerUsers.adapter = pagerAdapter

        // Link the tabs to the ViewPager
        TabLayoutMediator(binding.tabLayoutUsers, binding.viewPagerUsers) { tab, position ->
            tab.text = when (position) {
                0 -> "Customers"
                1 -> "Workers"
                2 -> "Admins"
                else -> null
            }
        }.attach()

        binding.fabAddUser.setOnClickListener {
            // Use the generated Directions class for type-safe navigation
            val action = AdminUsersFragmentDirections.actionNavAdminUsersToAdminAddUserFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}