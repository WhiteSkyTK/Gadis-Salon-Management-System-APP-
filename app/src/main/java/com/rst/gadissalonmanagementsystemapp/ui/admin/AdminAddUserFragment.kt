package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddUserBinding
import kotlinx.coroutines.launch

class AdminAddUserFragment : Fragment() {

    private var _binding: FragmentAdminAddUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveUserButton.setOnClickListener {
            saveUser()
        }
    }

    private fun saveUser() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        val role = when (binding.roleRadioGroup.checkedRadioButtonId) {
            binding.radioWorker.id -> "WORKER"
            binding.radioAdmin.id -> "ADMIN"
            else -> "CUSTOMER"
        }

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Show loading state ---
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveUserButton.isEnabled = false

        lifecycleScope.launch {
            val result = FirebaseManager.createUserByAdmin(name, email, phone, password, role)

            // --- Hide loading state ---
            binding.loadingIndicator.visibility = View.GONE
            binding.saveUserButton.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(context, "$role '$name' added successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}