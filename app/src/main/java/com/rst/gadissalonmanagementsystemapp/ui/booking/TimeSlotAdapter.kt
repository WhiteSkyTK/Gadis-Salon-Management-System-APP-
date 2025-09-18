package com.rst.gadissalonmanagementsystemapp.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.R

class TimeSlotAdapter(
    private val timeSlots: List<String>,
    private val availableSlots: List<String>,
    private val onTimeSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(time: String) {
            chip.text = time
            // Check if this time slot is in the list of available slots
            chip.isEnabled = availableSlots.contains(time)

            // Set the checked state based on the selected position
            chip.isChecked = (adapterPosition == selectedPosition)

            chip.setOnClickListener {
                if (chip.isEnabled) {
                    // Update the selected position and notify the adapter
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    // Report the selection back to the fragment
                    onTimeSelected(time)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false) as Chip
        return ViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(timeSlots[position])
    }

    override fun getItemCount(): Int = timeSlots.size
}