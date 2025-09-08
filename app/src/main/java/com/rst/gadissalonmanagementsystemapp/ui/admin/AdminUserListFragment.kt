package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminListBinding
import kotlinx.coroutines.launch

// This smart fragment can display a list of users filtered by their role.
class AdminUserListFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the user role passed from the PagerAdapter
        arguments?.let {
            userRole = it.getString("USER_ROLE")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adminRecyclerView.layoutManager = LinearLayoutManager(context)

        // Launch a coroutine to fetch the users from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getAllUsers()

            if (result.isSuccess) {
                val allUsers = result.getOrNull() ?: emptyList()

                // Filter the full list based on the role for this specific tab
                val filteredUsers = if (userRole != null) {
                    allUsers.filter { it.role.equals(userRole, ignoreCase = true) }
                } else {
                    allUsers
                }
                binding.adminRecyclerView.adapter = AdminUserAdapter(filteredUsers)
            } else {
                // Handle the error
                Toast.makeText(context, "Error fetching users: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}