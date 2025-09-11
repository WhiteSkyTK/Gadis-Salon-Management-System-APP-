package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditUserBinding
import kotlinx.coroutines.launch

class AdminEditUserFragment : Fragment() {
    private var _binding: FragmentAdminEditUserBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditUserFragmentArgs by navArgs()
    private var userToEdit: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData(args.userId)

        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUserData(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getUser(userId)
            if (result.isSuccess) {
                userToEdit = result.getOrNull()

                // If the user is found, populate the fields
                if (userToEdit != null) {
                    populateFields(userToEdit!!) // We can safely use !! here because of the null check
                } else {
                    Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } else {
                Toast.makeText(context, "Error loading user data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateFields(user: User) {
        binding.profileImagePreview.load(user.imageUrl) {
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }
        binding.emailText.text = user.email
        binding.nameInput.setText(user.name)
        binding.phoneInput.setText(user.phone)

        when (user.role.uppercase()) {
            "WORKER" -> binding.radioWorker.isChecked = true
            "ADMIN" -> binding.radioAdmin.isChecked = true
            else -> binding.radioCustomer.isChecked = true
        }
    }

    private fun saveChanges() {
        if (userToEdit == null) return

        val newName = binding.nameInput.text.toString().trim()
        val newPhone = binding.phoneInput.text.toString().trim()
        val newRole = when (binding.roleRadioGroup.checkedRadioButtonId) {
            binding.radioWorker.id -> "WORKER"
            binding.radioAdmin.id -> "ADMIN"
            else -> "CUSTOMER"
        }

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Create an updated User object
        val updatedUser = userToEdit!!.copy(
            name = newName,
            phone = newPhone,
            role = newRole
        )

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveChangesButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateUser(updatedUser)
            if (result.isSuccess) {
                Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                binding.saveChangesButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}