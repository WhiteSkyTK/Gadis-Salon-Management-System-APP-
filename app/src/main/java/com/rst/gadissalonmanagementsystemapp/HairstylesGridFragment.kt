package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentItemGridBinding
import com.rst.gadissalonmanagementsystemapp.ui.shop.ShopFragmentDirections

class HairstylesGridFragment : Fragment() {

    private var _binding: FragmentItemGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the LiveData from AppData
        AppData.allHairstyles.observe(viewLifecycleOwner) { hairstyleList ->
            // This code runs whenever the list of hairstyles changes.
            // We now pass the actual list (hairstyleList) to the adapter.
            val adapter = HairstyleItemAdapter(hairstyleList) { hairstyle ->
                findNavController().navigate(ShopFragmentDirections.actionShopFragmentToHairstyleDetailFragment(hairstyle))
            }
            binding.gridRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}