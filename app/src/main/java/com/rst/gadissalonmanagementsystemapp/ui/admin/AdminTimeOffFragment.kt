package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.rst.gadissalonmanagementsystemapp.AdminMainActivity
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.TimeOffRequest
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminTimeOffBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminTimeOffFragment : Fragment() {

    private var _binding: FragmentAdminTimeOffBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminTimeOffFragment"
    private lateinit var db: FirebaseFirestore
    private lateinit var timeOffAdapter: TimeOffAdapter
    private var timeOffListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminTimeOffBinding.inflate(inflater, container, false)
        db = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AdminMainActivity)?.showBackButton(true)
        (activity as? AdminMainActivity)?.updateTitle("Time Off Management")

        setupRecyclerView()

        binding.createTimeOffFab.setOnClickListener {
            // Navigate to the dialog to create a new request
            val action = AdminTimeOffFragmentDirections.actionAdminTimeOffFragmentToAdminTimeOffDialog(null)
            findNavController().navigate(action)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForTimeOffRequests()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        timeOffListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.shimmerViewContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
        binding.timeOffRecyclerView.visibility = if (isOffline) View.GONE else View.INVISIBLE
    }

    private fun setupRecyclerView() {
        timeOffAdapter = TimeOffAdapter(
            requireContext(),
            onApprove = { request -> updateRequestStatus(request.id, "approved") },
            onReject = { request -> updateRequestStatus(request.id, "rejected") },
            onEdit = { request ->
                // Navigate to the dialog to edit
                val action = AdminTimeOffFragmentDirections.actionAdminTimeOffFragmentToAdminTimeOffDialog(request)
                findNavController().navigate(action)
            },
            onDelete = { request -> deleteRequest(request.id) }
        )
        binding.timeOffRecyclerView.adapter = timeOffAdapter
    }

    private fun listenForTimeOffRequests() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.timeOffRecyclerView.visibility = View.INVISIBLE
        binding.emptyView.visibility = View.GONE

        val query = db.collection("timeOffRequests")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        timeOffListener = query.addSnapshotListener { snapshot, e ->
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                binding.emptyView.text = "Error loading requests. Please check Firestore index."
                binding.emptyView.visibility = View.VISIBLE
                binding.timeOffRecyclerView.visibility = View.GONE
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val requests = snapshot.toObjects<TimeOffRequest>()
                // Manually add the document ID to each request object
                requests.forEachIndexed { index, request ->
                    request.id = snapshot.documents[index].id
                }
                timeOffAdapter.submitList(requests)
                binding.timeOffRecyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else {
                Log.d(TAG, "No time off requests found")
                binding.timeOffRecyclerView.visibility = View.GONE
                binding.emptyView.text = "No time off requests found."
                binding.emptyView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateRequestStatus(requestId: String, newStatus: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("timeOffRequests").document(requestId)
                    .update("status", newStatus)
                    .await()
                Toast.makeText(context, "Request ${newStatus.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status for $requestId", e)
                Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRequest(requestId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Request")
            .setMessage("Are you sure you want to permanently delete this time off request?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        db.collection("timeOffRequests").document(requestId).delete().await()
                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting request $requestId", e)
                        Toast.makeText(context, "Failed to delete request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeOffListener?.remove()
        _binding = null
    }
}
