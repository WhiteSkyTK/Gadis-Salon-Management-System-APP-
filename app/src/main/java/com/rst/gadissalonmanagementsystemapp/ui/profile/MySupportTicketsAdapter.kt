package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rst.gadissalonmanagementsystemapp.SupportMessage
import com.rst.gadissalonmanagementsystemapp.databinding.ItemMySupportTicketBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MySupportTicketsAdapter(
    private var tickets: List<SupportMessage>,
    private val onItemClick: (SupportMessage) -> Unit
) : RecyclerView.Adapter<MySupportTicketsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMySupportTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: SupportMessage) {
            binding.messagePreviewText.text = ticket.message
            binding.statusChip.text = ticket.status
            // Format the timestamp into a readable date
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            binding.timestampText.text = "Sent: ${sdf.format(Date(ticket.timestamp))}"

            itemView.setOnClickListener { onItemClick(ticket) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMySupportTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int = tickets.size

    fun updateData(newTickets: List<SupportMessage>) {
        this.tickets = newTickets
        notifyDataSetChanged()
    }
}