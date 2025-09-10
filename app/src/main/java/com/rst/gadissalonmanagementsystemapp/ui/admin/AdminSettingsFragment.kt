package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.Loading
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentSettingsBinding

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // Set the switch to the correct initial state based on the current theme
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        binding.darkModeSwitch.isChecked = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        // Listen for changes
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            // Apply the new theme instantly
            AppCompatDelegate.setDefaultNightMode(newMode)
            // Save the user's choice so the app remembers it on the next launch
            prefs.edit().putInt("theme_mode", newMode).apply()
        }
    }

    private fun setupSignOutButton() {
        binding.signOutButton.setOnClickListener {
            // 1. Sign out from Firebase Authentication
            Firebase.auth.signOut()

            // 2. Clear the saved role from the cache (SharedPreferences)
            val prefs = requireActivity().getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(Loading.USER_ROLE_KEY).apply()

            // 3. Navigate back to the start of the app and clear all previous screens
            val intent = Intent(requireContext(), Loading::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish() // Finish the AdminMainActivity
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

