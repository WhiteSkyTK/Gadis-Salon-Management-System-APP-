package com.rst.gadissalonmanagementsystemapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.launch


class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationsAdapter
    private var notificationListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        notificationListener = FirebaseManager.addUserNotificationsListener { notifications ->
            if (view != null) {
                adapter.updateData(notifications)
            }
        }

        // --- THIS IS THE FIX ---
        // As soon as the screen is visible, mark all notifications as read.
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.markAllNotificationsAsRead()
        }
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(emptyList()) { notification ->
            //TODO In the future, you could navigate to the specific booking or order here
        }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.notificationsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}