package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminListBinding
import kotlinx.coroutines.launch

class AdminUserListFragment : Fragment() {
    private var _binding: FragmentAdminListBinding? = null
    private val binding get() = _binding!!
    private var userRole: String? = null
    private lateinit var adapter: AdminUserAdapter

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
        setupRecyclerView()
        listenForUserUpdates()
    }

    private fun setupRecyclerView() {
        adapter = AdminUserAdapter(
            users = emptyList(),
            onEditClick = { user ->
                val action = AdminUsersFragmentDirections.actionNavAdminUsersToAdminEditUserFragment(user.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { user ->
                confirmAndDeleteUser(user)
            }
        )
        binding.adminRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.adminRecyclerView.adapter = adapter
    }

    private fun listenForUserUpdates() {
        FirebaseManager.addUserListener { allUsers ->
            val filteredUsers = if (userRole != null) {
                allUsers.filter { it.role.equals(userRole, ignoreCase = true) }
            } else {
                allUsers
            }
            // The adapter will automatically update with the new list
            adapter.updateData(filteredUsers)
        }
    }

    private fun confirmAndDeleteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Call the FirebaseManager to delete from Firestore
                    FirebaseManager.deleteUser(user.id)
                    // TODO: Also add a function to delete the user from Firebase Authentication
                    Toast.makeText(context, "${user.name} deleted.", Toast.LENGTH_SHORT).show()
                    // The real-time listener will automatically refresh the list
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}