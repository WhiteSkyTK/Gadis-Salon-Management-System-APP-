package com.rst.gadissalonmanagementsystemapp.ui.worker

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

class WorkerSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSwitch()
    }

    private fun setupThemeSwitch() {
        val prefs = requireActivity().getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
        binding.darkModeSwitch.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newMode)
            prefs.edit().putInt("theme_mode", newMode).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}