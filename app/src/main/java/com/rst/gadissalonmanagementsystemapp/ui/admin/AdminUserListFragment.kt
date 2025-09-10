package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
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

class AdminUserListFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!
    private var userRole: String? = null

    // A TAG for filtering our logs
    companion object {
        private const val TAG = "AdminUserListFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        Log.d(TAG, "Fragment created for role: $userRole")

        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Fetching users from Firebase...")
            val result = FirebaseManager.getAllUsers()

            if (result.isSuccess) {
                val allUsers = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Successfully fetched ${allUsers.size} total users from Firebase.")

                val filteredUsers = if (userRole != null) {
                    allUsers.filter { it.role.equals(userRole, ignoreCase = true) }
                } else {
                    allUsers
                }
                Log.d(TAG, "Filtered list contains ${filteredUsers.size} users for role '$userRole'.")

                binding.adminRecyclerView.adapter = AdminUserAdapter(filteredUsers)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Error fetching users: $errorMessage")
                Toast.makeText(context, "Error fetching users: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}