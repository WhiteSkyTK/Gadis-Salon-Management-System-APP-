package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.FaqItem
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.DialogAddFaqBinding
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminFaqBinding
import kotlinx.coroutines.launch

class AdminFaqFragment : Fragment() {
    private var _binding: FragmentAdminFaqBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminFaqAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForFaqUpdates()

        binding.fabAddFaq.setOnClickListener {
            showAddFaqDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminFaqAdapter(
            faqList = emptyList(),
            onDelete = { faqItem ->
                // Call the FirebaseManager to delete the item
                lifecycleScope.launch {
                    FirebaseManager.deleteFaq(faqItem.id)
                }
            }
        )
        binding.adminFaqRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.adminFaqRecyclerView.adapter = adapter
    }

    private fun listenForFaqUpdates() {
        FirebaseManager.addFaqListener { faqList ->
            adapter.updateData(faqList)
        }
    }

    private fun showAddFaqDialog() {
        val dialogBinding = DialogAddFaqBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Add New FAQ")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val question = dialogBinding.questionInput.text.toString().trim()
                val answer = dialogBinding.answerInput.text.toString().trim()
                if (question.isNotBlank() && answer.isNotBlank()) {
                    lifecycleScope.launch {
                        FirebaseManager.addFaq(FaqItem(question = question, answer = answer))
                        Toast.makeText(context, "FAQ added successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
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