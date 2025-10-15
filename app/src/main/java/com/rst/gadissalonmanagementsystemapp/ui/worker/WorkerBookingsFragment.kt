package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.AdminBooking
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerBookingsBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils // --- IMPORT THE UTILITY ---
import kotlinx.coroutines.launch


class WorkerBookingsFragment : Fragment() {
    private var _binding: FragmentWorkerBookingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WorkerBookingAdapter
    private val mainViewModel: MainViewModel by activityViewModels()
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        // Check for internet before fetching data
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForPendingBookings()
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
        adapter = WorkerBookingAdapter(
            bookings = mutableListOf(),
            allHairstyles = mainViewModel.allHairstyles.value ?: emptyList(),
            onAccept = { booking ->
                acceptBooking(booking)
            },
            onDecline = { booking ->
                declineBooking(booking)
            },
            onItemClick = { booking ->
                val action = WorkerBookingsFragmentDirections.actionNavWorkerBookingsToBookingDetailWorkerFragment(booking)
                findNavController().navigate(action)
            }
        )
        binding.workerBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.workerBookingsRecyclerView.adapter = adapter
    }

    private fun listenForPendingBookings() {
        // --- START SHIMMER ---
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.workerBookingsRecyclerView.visibility = View.GONE
        binding.emptyViewText.visibility = View.GONE

        bookingsListener = FirebaseManager.addPendingBookingsListener { pendingBookings ->
            if (view == null) return@addPendingBookingsListener

            // --- STOP SHIMMER ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (pendingBookings.isEmpty()) {
                // Show empty message
                binding.workerBookingsRecyclerView.visibility = View.GONE
                binding.emptyViewText.visibility = View.VISIBLE
            } else {
                // Show the list
                binding.workerBookingsRecyclerView.visibility = View.VISIBLE
                binding.emptyViewText.visibility = View.GONE
                adapter.updateData(pendingBookings)
            }
        }
    }

    private fun acceptBooking(booking: AdminBooking) {
        viewLifecycleOwner.lifecycleScope.launch {
            val workerResult = FirebaseManager.getCurrentUser()
            if (workerResult.isSuccess && workerResult.getOrNull() != null) {
                val currentWorker = workerResult.getOrNull()!!
                val result = FirebaseManager.acceptBooking(booking.id, currentWorker)
                if (result.isSuccess) {
                    Toast.makeText(context, "Booking Accepted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to accept booking.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error: Could not identify current worker.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun declineBooking(booking: AdminBooking) {
        val uid = Firebase.auth.currentUser?.uid ?: return

        if (booking.stylistName == "Any Available") {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = FirebaseManager.addDeclinedStylist(booking.id, uid)
                if (result.isSuccess) {
                    Toast.makeText(context, "Booking passed to other stylists.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to decline booking.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            showDeclineReasonDialog(booking)
        }
    }

    private fun showDeclineReasonDialog(booking: AdminBooking) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Reason for declining"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Decline Booking")
            .setMessage("Please provide a reason for declining this booking.")
            .setView(input)
            .setPositiveButton("Decline") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = FirebaseManager.declineDirectBooking(booking.id, reason)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Booking Declined", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to decline booking.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "A reason is required to decline.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

