package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerBookingBinding

class WorkerBookingAdapter(
    private var bookings: MutableList<AdminBooking>,
    private val allHairstyles: List<Hairstyle>,
    private val onAccept: (AdminBooking) -> Unit,
    private val onDecline: (AdminBooking) -> Unit,
    private val onItemClick: (AdminBooking) -> Unit // <-- Add this new parameter
) : RecyclerView.Adapter<WorkerBookingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            val hairstyle = allHairstyles.find { it.id == booking.hairstyleId }
            binding.hairstyleImageWorker.load(hairstyle?.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_placeholder_image)
            }

            binding.serviceName.text = booking.serviceName
            binding.customerName.text = "For: ${booking.customerName}"
            binding.bookingDateTime.text = "On: ${booking.date} at ${booking.time}"

            binding.acceptButton.setOnClickListener { onAccept(booking) }
            binding.declineButton.setOnClickListener { onDecline(booking) }
            itemView.setOnClickListener {
                onItemClick(booking)
            }
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
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }
}