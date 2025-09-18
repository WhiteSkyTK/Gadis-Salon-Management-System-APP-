package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAboutUsBinding
import kotlinx.coroutines.launch

class AboutUsFragment : Fragment() {

    private var _binding: FragmentAboutUsBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AboutUsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Launch a coroutine to fetch the content from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching About Us content from Firebase...")
            val result = FirebaseManager.getAboutUsContent()

            if (result.isSuccess) {
                val content = result.getOrNull()
                if (content != null) {
                    // Update the UI with the fetched text
                    binding.salonAboutText.text = content["salon_about"]
                    binding.appAboutText.text = content["app_about"]
                    Log.d(TAG, "Successfully displayed About Us content.")
                } else {
                    Log.w(TAG, "About Us content was null.")
                }
            } else {
                Log.e(TAG, "Error fetching About Us content: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}