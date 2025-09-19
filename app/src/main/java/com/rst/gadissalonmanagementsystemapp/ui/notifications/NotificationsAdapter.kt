package com.rst.gadissalonmanagementsystemapp.ui.notifications

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.AppNotification
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.ItemNotificationBinding
import java.util.concurrent.TimeUnit

class NotificationsAdapter(
    private var notifications: List<AppNotification>,
    private val onItemClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: AppNotification) {
            binding.notificationTitle.text = notification.title
            binding.notificationBody.text = notification.message
            binding.notificationTime.text = getTimeAgo(notification.timestamp)

            // Make unread notifications bold for emphasis
            val textStyle = if (notification.isRead) Typeface.NORMAL else Typeface.BOLD
            binding.notificationTitle.setTypeface(null, textStyle)
            binding.notificationBody.setTypeface(null, textStyle)

            // Set the icon based on the notification type (optional but good for UX)
            val iconRes = when {
                notification.title.contains("Booking", ignoreCase = true) -> R.drawable.ic_booking
                notification.title.contains("Order", ignoreCase = true) -> R.drawable.ic_orders
                else -> R.drawable.ic_notifications
            }
            binding.notificationIcon.setImageResource(iconRes)

            itemView.setOnClickListener {
                onItemClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<AppNotification>) {
        this.notifications = newNotifications
        notifyDataSetChanged()
    }

    // Helper function to calculate a "time ago" string
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
    }
}