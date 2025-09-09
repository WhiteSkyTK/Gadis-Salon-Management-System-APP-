package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminBookingBinding

class AdminBookingAdapter(private val bookings: List<AdminBooking>) : RecyclerView.Adapter<AdminBookingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            binding.serviceName.text = booking.serviceName
            binding.customerNameValue.text = booking.customerName
            binding.stylistNameValue.text = booking.stylistName
            binding.bookingDateAdmin.text = "${booking.date}, ${booking.time}"
            binding.bookingStatusAdmin.text = booking.status

            // Set the status color
            val statusColor = when (booking.status.lowercase()) {
                "completed" -> R.color.status_green
                "cancelled" -> R.color.status_red
                "confirmed" -> R.color.colorPrimary
                else -> R.color.status_grey // Pending
            }
            binding.bookingStatusAdmin.background.setTint(ContextCompat.getColor(itemView.context, statusColor))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size
}