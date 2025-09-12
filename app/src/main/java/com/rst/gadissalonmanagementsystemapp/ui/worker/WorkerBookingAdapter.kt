package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerBookingBinding

class WorkerBookingAdapter(
    private val bookings: List<AdminBooking>,
    private val onAccept: (AdminBooking) -> Unit,
    private val onDecline: (AdminBooking) -> Unit
) : RecyclerView.Adapter<WorkerBookingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            binding.serviceName.text = booking.serviceName
            binding.customerName.text = "For: ${booking.customerName}"
            binding.bookingDateTime.text = "On: ${booking.date} at ${booking.time}"

            binding.acceptButton.setOnClickListener { onAccept(booking) }
            binding.declineButton.setOnClickListener { onDecline(booking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkerBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateData(newBookings: List<AdminBooking>) {
        (this.bookings as MutableList).clear()
        (this.bookings as MutableList).addAll(newBookings)
        notifyDataSetChanged()
    }
}