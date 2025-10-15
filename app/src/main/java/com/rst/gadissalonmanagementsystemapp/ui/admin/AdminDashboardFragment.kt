package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminDashboardBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminDashboard"

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
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Check for internet before fetching data
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            setupDashboardStats()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupDashboardStats() {
        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE // Keep layout space but hide content

        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching dashboard stats from Firebase...")

            val userResult = FirebaseManager.getAllUsers()
            val bookingResult = FirebaseManager.getAllBookings()
            val productResult = FirebaseManager.getAllProducts()

            if (!isAdded) return@launch

            // --- STOP SHIMMER & SHOW CONTENT ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE

            // --- Process Users ---
            if (userResult.isSuccess) {
                val allUsers = userResult.getOrNull() ?: emptyList()

                // --- THIS IS THE NEW LOGIC ---
                // Find the current admin's name and update the welcome text
                val currentAdminUid = Firebase.auth.currentUser?.uid
                val currentAdmin = allUsers.find { it.id == currentAdminUid }
                if (currentAdmin != null) {
                    binding.welcomeText.text = "Hello, ${currentAdmin.name}"
                }
                // --- END OF NEW LOGIC ---

                startCountUpAnimation(binding.customersCountText, allUsers.count { it.role == "CUSTOMER" })
                startCountUpAnimation(binding.stylistsCountText, allUsers.count { it.role == "WORKER" })
            }

            // --- Process Bookings ---
            if (bookingResult.isSuccess) {
                val allBookings = bookingResult.getOrNull() ?: emptyList()
                startCountUpAnimation(binding.bookingsCountText, allBookings.size)
            }

            // --- Process Products ---
            if (productResult.isSuccess) {
                val allProducts = productResult.getOrNull() ?: emptyList()
                val totalStock = allProducts.sumOf { p -> p.variants.sumOf { v -> v.stock } }
                startCountUpAnimation(binding.totalStockCountText, totalStock)
            }
        }
    }

    private fun startCountUpAnimation(textView: TextView, finalValue: Int) {
        val animator = ValueAnimator.ofInt(0, finalValue)
        animator.duration = 1500 // Animation duration in milliseconds
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
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