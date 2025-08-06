package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Set Fake User Data ---
        // Using your name, TK, as a placeholder
        binding.userName.text = "Tokollo (TK)"
        binding.userPhone.text = "+27 12 345 6789"
        binding.profileImage.setImageResource(R.drawable.ic_profile) // Or a real image later

        // --- 2. Get App Version Programmatically ---
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            binding.versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- 3. Setup Click Listeners for Options ---
        binding.settingsOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.aboutUsOption.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutUsFragment)
        }
        // Add listeners for contact and location here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}