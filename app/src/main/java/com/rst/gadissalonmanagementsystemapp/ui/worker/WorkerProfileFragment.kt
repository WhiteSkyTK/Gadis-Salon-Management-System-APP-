package com.rst.gadissalonmanagementsystemapp.ui.worker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Loading
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentWorkerProfileBinding
import kotlinx.coroutines.launch

class WorkerProfileFragment : Fragment() {

    private var _binding: FragmentWorkerProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadWorkerProfile()
        setupClickListeners()
    }

    private fun loadWorkerProfile() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getUser(uid)
            if (result.isSuccess) {
                val worker = result.getOrNull()
                binding.userNameWorker.text = worker?.name ?: "Stylist"
                binding.userPhoneWorker.text = worker?.phone ?: "No phone number"
                binding.profileImageWorker.load(worker?.imageUrl) {
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.contactSupportOption.setOnClickListener {
            findNavController().navigate(R.id.action_workerProfile_to_workerHelp)
        }
        binding.settingsOption.setOnClickListener {
            findNavController().navigate(R.id.action_workerProfile_to_workerSettings)
        }
        binding.locationOption.setOnClickListener {
            findNavController().navigate(R.id.action_workerProfile_to_workerLocation)
        }
        binding.faqOption.setOnClickListener {
            findNavController().navigate(R.id.action_workerProfile_to_workerFaq)
        }
        binding.aboutUsOption.setOnClickListener {
            findNavController().navigate(R.id.action_workerProfile_to_workerAbout)
        }
        binding.logOutButton.setOnClickListener {
            Firebase.auth.signOut()
            val prefs =
                requireActivity().getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(Loading.USER_ROLE_KEY).apply()
            val intent = Intent(requireContext(), Loading::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}