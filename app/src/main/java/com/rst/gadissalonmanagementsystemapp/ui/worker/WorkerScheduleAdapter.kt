package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerScheduleBinding

class WorkerScheduleAdapter(private val bookings: List<AdminBooking>) : RecyclerView.Adapter<WorkerScheduleAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            // In a real app, you would calculate the end time based on duration
            binding.timeText.text = "${booking.time} - 15:00"
            binding.serviceNameText.text = booking.serviceName
            binding.customerNameText.text = "with ${booking.customerName}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkerScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size
}