package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rst.gadissalonmanagementsystemapp.FaqItem
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentFaqBinding

class FaqFragment : Fragment() {
    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val faqList = listOf(
            FaqItem("How do I book an appointment?", "You can book an appointment by selecting a hairstyle from the Home or Shop screen, tapping 'Book Now', and following the steps on the confirmation page."),
            FaqItem("What are the salon's operating hours?", "Our salon is open from 9:00 AM to 5:00 PM, Monday to Saturday."),
            FaqItem("How can I cancel a booking?", "To cancel a booking, go to the 'My Bookings' screen, find your upcoming appointment, and tap the 'Cancel' option.")
        )

        binding.faqRecyclerView.adapter = FaqAdapter(faqList)
    }
}