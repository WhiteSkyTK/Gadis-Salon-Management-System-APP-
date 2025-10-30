package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.TimeOffRequest
import com.rst.gadissalonmanagementsystemapp.databinding.ItemTimeOffAdminBinding

class TimeOffAdapter(
    private val context: Context,
    private val onApprove: (TimeOffRequest) -> Unit,
    private val onReject: (TimeOffRequest) -> Unit,
    private val onEdit: (TimeOffRequest) -> Unit,
    private val onDelete: (TimeOffRequest) -> Unit
) : ListAdapter<TimeOffRequest, TimeOffAdapter.TimeOffViewHolder>(TimeOffDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeOffViewHolder {
        val binding = ItemTimeOffAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeOffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeOffViewHolder, position: Int) {
        val request = getItem(position)
        holder.bind(request)
    }

    inner class TimeOffViewHolder(private val binding: ItemTimeOffAdminBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: TimeOffRequest) {
            binding.stylistNameText.text = request.stylistName
            binding.dateRangeText.text = "${request.startDate} to ${request.endDate}"
            binding.reasonText.text = request.reason
            binding.statusBadge.text = request.status.replaceFirstChar { it.uppercase() }

            // Set status badge color
            when (request.status.lowercase()) {
                "pending" -> {
                    binding.statusBadge.setBackgroundResource(R.drawable.status_badge_pending)
                    binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.darkYellow))
                    binding.pendingActionsLayout.visibility = View.VISIBLE
                }
                "approved" -> {
                    binding.statusBadge.setBackgroundResource(R.drawable.status_badge_confirmed)
                    binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.darkGreen))
                    binding.pendingActionsLayout.visibility = View.GONE
                }
                "rejected" -> {
                    binding.statusBadge.setBackgroundResource(R.drawable.status_badge_cancelled)
                    binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.darkRed))
                    binding.pendingActionsLayout.visibility = View.GONE
                }
                else -> {
                    binding.statusBadge.setBackgroundResource(R.drawable.status_badge_default)
                    binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.darkGrey))
                    binding.pendingActionsLayout.visibility = View.GONE
                }
            }

            // Set click listeners
            binding.approveButton.setOnClickListener { onApprove(request) }
            binding.rejectButton.setOnClickListener { onReject(request) }
            binding.editButton.setOnClickListener { onEdit(request) }
            binding.deleteButton.setOnClickListener { onDelete(request) }
        }
    }

    class TimeOffDiffCallback : DiffUtil.ItemCallback<TimeOffRequest>() {
        override fun areItemsTheSame(oldItem: TimeOffRequest, newItem: TimeOffRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimeOffRequest, newItem: TimeOffRequest): Boolean {
            return oldItem == newItem
        }
    }
}
