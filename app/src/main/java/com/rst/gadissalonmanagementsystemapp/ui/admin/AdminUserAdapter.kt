package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.User
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminUserBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminUserAdapter(
    private var users: List<User>,
    private val onEditClick: (User) -> Unit, // For editing
    private val onDeleteClick: (User) -> Unit  // For deleting
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userName.text = user.name
            binding.userEmail.text = user.email
            binding.userRoleChip.text = user.role.uppercase()
            binding.userImage.load(user.imageUrl) {
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }

            binding.editButton.setOnClickListener {
                onEditClick(user)
            }
            binding.deleteButton.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to permanently delete ${user.name}?")
                    .setPositiveButton("Delete") { _, _ -> onDeleteClick(user) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged() // This tells the RecyclerView to redraw itself
    }
}