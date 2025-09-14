package com.rst.gadissalonmanagementsystemapp.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCustomerOrdersBinding

class CustomerOrdersFragment : Fragment() {
    private var _binding: FragmentCustomerOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }
}