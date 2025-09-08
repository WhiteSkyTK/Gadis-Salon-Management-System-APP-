package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.R

class TimeSlotAdapter(private val timeSlots: List<String>) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    inner class TimeSlotViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false) as Chip
        return TimeSlotViewHolder(chip)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.chip.text = timeSlots[position]
    }

    override fun getItemCount(): Int = timeSlots.size
}