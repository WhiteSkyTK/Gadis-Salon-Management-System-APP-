package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemWorkerScheduleBinding

class WorkerScheduleAdapter(
    private var bookings: List<AdminBooking>,
    private val allHairstyles: List<Hairstyle>,
    private val onItemClick: (AdminBooking) -> Unit
) : RecyclerView.Adapter<WorkerScheduleAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemWorkerScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: AdminBooking) {
            val hairstyle = allHairstyles.find { it.id == booking.hairstyleId }

            // Load the image with Coil
            binding.hairstyleImage.load(hairstyle?.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }

            val duration = hairstyle?.durationHours ?: 1
            val startTimeHour = booking.time.split(":")[0].toInt()
            val endTimeHour = startTimeHour + duration

            binding.timeText.text = "${booking.time} - $endTimeHour:00"
            binding.serviceNameText.text = booking.serviceName
            binding.customerNameText.text = "with ${booking.customerName}"

            // Show or hide the dot based on the unread count from Firebase
            binding.unreadMessageDot.visibility = if (booking.workerUnreadCount > 0) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onItemClick(booking)
            }
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

    fun updateData(newBookings: List<AdminBooking>) {
        this.bookings = newBookings
        notifyDataSetChanged()
    }
}