package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.Hairstyle
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemBookingBinding

class BookingAdapter(
    private var bookings: List<AdminBooking>,
    private val allHairstyles: List<Hairstyle>,
    private val onItemClick: (AdminBooking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    inner class BookingViewHolder(private val binding: ItemBookingBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: AdminBooking) {
            val hairstyle = allHairstyles.find { it.id == booking.hairstyleId }
            binding.styleImage.load(hairstyle?.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
            }

            binding.styleName.text = booking.serviceName
            binding.stylistName.text = "with ${booking.stylistName}"
            binding.bookingDate.text = booking.date
            binding.bookingTime.text = "at ${booking.time}"
            binding.bookingStatus.text = booking.status

            // 1. Get the correct color resource for the status
            val statusColorRes = when (booking.status.lowercase()) {
                "completed" -> R.color.status_green
                "cancelled" -> R.color.status_red
                "confirmed" -> R.color.colorPrimary2
                else -> R.color.status_grey
            }

            val color = ContextCompat.getColor(itemView.context, statusColorRes)
            (binding.bookingStatus.background.mutate() as? GradientDrawable)?.setColor(color)

            itemView.setOnClickListener {
                onItemClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateData(newBookings: List<AdminBooking>) {
        Log.d("BookingAdapter", "updateData called with ${newBookings.size} items. Refreshing UI.")
        this.bookings = newBookings
        notifyDataSetChanged()
    }
}