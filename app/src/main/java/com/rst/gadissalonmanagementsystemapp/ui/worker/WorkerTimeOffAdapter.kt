package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.TimeOffRequest
import com.rst.gadissalonmanagementsystemapp.databinding.ItemTimeOffWorkerBinding

class WorkerTimeOffAdapter(
    private val onDeleteClick: (TimeOffRequest) -> Unit
) : ListAdapter<TimeOffRequest, WorkerTimeOffAdapter.ViewHolder>(TimeOffDiffCallback()) {

    inner class ViewHolder(private val binding: ItemTimeOffWorkerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: TimeOffRequest) {
            binding.dateRangeText.text = "${request.startDate} to ${request.endDate}"
            binding.reasonText.text = request.reason
            binding.statusText.text = request.status.replaceFirstChar { it.uppercase() }

            val context = binding.root.context
            when (request.status.lowercase()) {
                "pending" -> {
                    binding.statusText.setChipBackgroundColorResource(R.color.status_grey)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
                    binding.deleteButton.visibility = View.VISIBLE // Can delete pending requests
                }
                "approved" -> {
                    binding.statusText.setChipBackgroundColorResource(R.color.status_green_bg)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_green))
                    binding.deleteButton.visibility = View.GONE
                }
                "rejected" -> {
                    binding.statusText.setChipBackgroundColorResource(R.color.status_red_bg)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_red))
                    binding.deleteButton.visibility = View.GONE
                }
                else -> {
                    binding.statusText.setChipBackgroundColorResource(R.color.status_grey)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
                    binding.deleteButton.visibility = View.GONE
                }
            }

            binding.deleteButton.setOnClickListener { onDeleteClick(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimeOffWorkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
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
