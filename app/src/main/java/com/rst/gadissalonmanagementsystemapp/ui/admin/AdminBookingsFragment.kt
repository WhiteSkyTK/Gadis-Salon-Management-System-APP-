package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminGenericListBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils

class AdminBookingsFragment : Fragment() {
    private var _binding: FragmentAdminGenericListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminBookingAdapter
    private val mainViewModel: MainViewModel by activityViewModels()
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminGenericListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        val inflater = LayoutInflater.from(context)
        binding.placeholderContainer.removeAllViews() // Clear any old placeholders
        repeat(5) {
            val placeholder = inflater.inflate(R.layout.item_admin_booking_placeholder, binding.placeholderContainer, false)
            binding.placeholderContainer.addView(placeholder)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForBookingUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        bookingsListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = AdminBookingAdapter(
            bookings = emptyList(),
            allHairstyles = mainViewModel.allHairstyles.value ?: emptyList(),
            onItemClick = { booking ->
                // Navigate to the detail screen when a booking is clicked
                val action = AdminSalesFragmentDirections.actionAdminSalesFragmentToAdminBookingDetailFragment(booking)
                findNavController().navigate(action)
            }
        )
        binding.listRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.listRecyclerView.adapter = adapter
    }

    private fun listenForBookingUpdates() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.listRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        bookingsListener = FirebaseManager.addBookingsListener { bookingsList ->
            if (view == null) return@addBookingsListener

            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (bookingsList.isEmpty()) {
                binding.listRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
                binding.emptyViewText.text = "No bookings found."
            } else {
                binding.listRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                adapter.updateData(bookingsList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
