package com.rst.gadissalonmanagementsystemapp.ui.worker

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.TimeOffRequest
import com.rst.gadissalonmanagementsystemapp.WorkerMainActivity
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerTimeOffBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WorkerTimeOffFragment : Fragment() {

    private var _binding: FragmentWorkerTimeOffBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private val TAG = "WorkerTimeOffFragment"
    private lateinit var timeOffAdapter: WorkerTimeOffAdapter
    private var requestsListener: ListenerRegistration? = null
    private val currentUserId = Firebase.auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerTimeOffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update Activity Title
        (activity as? WorkerMainActivity)?.updateTitle("My Time Off")

        setupRecyclerView()
        setupListeners()
        observeTimeOffRequests()
    }

    private fun setupRecyclerView() {
        timeOffAdapter = WorkerTimeOffAdapter { request ->
            // Only allow deleting pending requests
            if (request.status == "pending") {
                deleteRequest(request)
            } else {
                Toast.makeText(context, "Only pending requests can be deleted.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.timeOffRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timeOffAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddTimeOff.setOnClickListener {
            findNavController().navigate(R.id.action_workerTimeOffFragment_to_workerTimeOffDialog)
        }
    }

    private fun observeTimeOffRequests() {
        if (currentUserId == null) {
            Log.e(TAG, "User is not logged in. Cannot fetch requests.")
            return
        }

        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.timeOffRecyclerView.visibility = View.GONE

        // This query requires a Firestore index.
        val query = db.collection("timeOffRequests")
            .whereEqualTo("stylistId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        requestsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching time off requests", error)
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                // Handle the index error specifically
                if (error.code.toString() == "FAILED_PRECONDITION") {
                    binding.textEmptyView.text = "Error: Missing database index. Please check logs and contact admin."
                    binding.textEmptyView.visibility = View.VISIBLE
                } else {
                    binding.textEmptyView.text = "Error loading requests."
                    binding.textEmptyView.visibility = View.VISIBLE
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val requests = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TimeOffRequest::class.java)?.copy(id = doc.id)
                }
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE

                if (requests.isEmpty()) {
                    binding.textEmptyView.text = "You have no time off requests."
                    binding.textEmptyView.visibility = View.VISIBLE
                    binding.timeOffRecyclerView.visibility = View.GONE
                } else {
                    binding.textEmptyView.visibility = View.GONE
                    binding.timeOffRecyclerView.visibility = View.VISIBLE
                    timeOffAdapter.submitList(requests)
                }
            }
        }
    }

    private fun deleteRequest(request: TimeOffRequest) {
        if (request.id.isEmpty()) {
            Log.e(TAG, "Cannot delete request with empty ID.")
            Toast.makeText(context, "Error: Cannot delete request.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Request")
            .setMessage("Are you sure you want to delete this time off request?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("timeOffRequests").document(request.id).delete().await()
                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting request", e)
                        Toast.makeText(context, "Failed to delete request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onStop() {
        super.onStop()
        requestsListener?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
