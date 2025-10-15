package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.CustomerMainActivity
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.WorkerMainActivity
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHelpCenterBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils

class HelpCenterFragment : Fragment() {
    private var _binding: FragmentHelpCenterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHelpCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Check for internet every time the fragment becomes visible
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false) // Hide offline screen
        } else {
            showOfflineUI(true) // Show offline screen
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.viewMyTicketsOption.setOnClickListener {
            // Check which activity is hosting this fragment
            when (requireActivity()) {
                is CustomerMainActivity -> findNavController().navigate(R.id.action_helpCenterFragment_to_mySupportTicketsFragment)
                is WorkerMainActivity -> findNavController().navigate(R.id.action_helpCenterFragment_to_mySupportTicketsFragment)
            }
        }

        binding.submitNewTicketOption.setOnClickListener {
            // Check which activity is hosting this fragment
            when (requireActivity()) {
                is CustomerMainActivity -> findNavController().navigate(R.id.action_helpCenterFragment_to_contactFragment)
                is WorkerMainActivity -> findNavController().navigate(R.id.action_helpCenterFragment_to_contactFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}