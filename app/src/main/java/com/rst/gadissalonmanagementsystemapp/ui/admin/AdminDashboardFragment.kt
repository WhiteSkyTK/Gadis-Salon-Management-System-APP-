package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminDashboardBinding
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDashboardStats()
        setupClickListeners()
    }

    private fun setupDashboardStats() {
        viewLifecycleOwner.lifecycleScope.launch {

            // --- Fetch Users ---
            val userResult = FirebaseManager.getAllUsers()
            if (userResult.isSuccess) {
                val allUsers = userResult.getOrNull() ?: emptyList()
                // Count customers
                val customerCount = allUsers.count { it.role.equals("CUSTOMER", ignoreCase = true) }
                binding.customersCountText.text = customerCount.toString()
                // Count workers/stylists
                val stylistCount = allUsers.count { it.role.equals("WORKER", ignoreCase = true) }
                binding.stylistsCountText.text = stylistCount.toString()
            } else {
                Toast.makeText(context, "Error fetching users: ${userResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }

            // --- Fetch Bookings ---
            val bookingResult = FirebaseManager.getAllBookings()
            if (bookingResult.isSuccess) {
                val allBookings = bookingResult.getOrNull() ?: emptyList()
                binding.bookingsCountText.text = allBookings.size.toString()
            } else {
                Toast.makeText(context, "Error fetching bookings: ${bookingResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.addProductButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminAddProductFragment)
        }
        binding.addWorkerButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminAddUserFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}