package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.Loading
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSwitch()
        setupSignOutButton()
    }

    private fun setupThemeSwitch() {
        val prefs = requireActivity().getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)

        // Set the switch to the correct initial state based on the saved preference
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        binding.darkModeSwitch.isChecked = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            // Apply the new theme
            AppCompatDelegate.setDefaultNightMode(newMode)
            // Save the user's choice for the next time they open the app
            prefs.edit().putInt("theme_mode", newMode).apply()
        }
    }

    private fun setupSignOutButton() {
        binding.signOutButton.setOnClickListener {
            // 1. Clear the user session from AppData
            AppData.logoutUser()

            // 2. Clear the saved login state from SharedPreferences
            val prefs = requireActivity().getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(Loading.USER_ROLE_KEY).apply()

            // 3. Navigate back to the Loading screen and clear all previous screens
            val intent = Intent(requireContext(), Loading::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}