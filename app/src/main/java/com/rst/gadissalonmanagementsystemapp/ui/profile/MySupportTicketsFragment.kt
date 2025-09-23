package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentMySupportTicketsBinding

class MySupportTicketsFragment : Fragment() {
    private var _binding: FragmentMySupportTicketsBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketsAdapter: MySupportTicketsAdapter
    private var ticketsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMySupportTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for real-time updates when the screen is visible
        ticketsListener = FirebaseManager.addCurrentUserSupportMessagesListener { myTickets ->
            if (view != null) {
                if (myTickets.isEmpty()) {
                    binding.emptyViewText.visibility = View.VISIBLE
                    binding.myTicketsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyViewText.visibility = View.GONE
                    binding.myTicketsRecyclerView.visibility = View.VISIBLE
                    ticketsAdapter.updateData(myTickets)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is not visible to prevent crashes
        ticketsListener?.remove()
    }

    private fun setupRecyclerView() {
        ticketsAdapter = MySupportTicketsAdapter(
            tickets = emptyList(),
            onItemClick = { ticket ->
                // This is what happens when a user clicks on a ticket
                val action = MySupportTicketsFragmentDirections.actionMySupportTicketsFragmentToTicketDetailFragment(ticket)
                findNavController().navigate(action)
            }
        )
        binding.myTicketsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.myTicketsRecyclerView.adapter = ticketsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}