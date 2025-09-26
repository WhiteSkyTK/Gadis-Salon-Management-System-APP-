package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemAdminBookingBinding

class AdminBookingAdapter(
    private var bookings: List<AdminBooking>,
    private val allHairstyles: List<Hairstyle> // Takes the master list
) : RecyclerView.Adapter<AdminBookingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            val hairstyle = allHairstyles.find { it.id == booking.hairstyleId }
            binding.hairstyleImageAdmin.load(hairstyle?.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }

            binding.serviceName.text = booking.serviceName
            binding.customerNameValue.text = booking.customerName
            binding.stylistNameValue.text = booking.stylistName
            binding.bookingDateAdmin.text = "${booking.date}, ${booking.time}"
            binding.bookingStatusAdmin.text = booking.status

            val statusColor = when (booking.status.lowercase()) {
                "completed" -> R.color.status_green
                "cancelled", "declined" -> R.color.status_red
                "confirmed" -> R.color.colorPrimary2
                else -> R.color.status_grey // Pending
            }
            // Use setChipBackgroundColorResource for Material Chips
            binding.bookingStatusAdmin.setChipBackgroundColorResource(statusColor)
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

    fun updateData(newBookings: List<AdminBooking>) {
        this.bookings = newBookings
        notifyDataSetChanged() // Refreshes the list
    }
}