package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentMySupportTicketsBinding

class MySupportTicketsFragment : Fragment() {
    private var _binding: FragmentMySupportTicketsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMySupportTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.myTicketsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Listen for real-time updates to the current user's tickets
        FirebaseManager.addCurrentUserSupportMessagesListener { myTickets ->
            binding.myTicketsRecyclerView.adapter = MySupportTicketsAdapter(myTickets)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}