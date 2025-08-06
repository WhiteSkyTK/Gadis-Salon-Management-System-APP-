package com.rst.gadissalonmanagementsystemapp.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.HomeItemAdapter
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentItemGridBinding

class HairstylesGridFragment : Fragment() {

    private var _binding: FragmentItemGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dummyHairstyles = listOf(
            Product("Butterfly Locs", "R450", Product.TYPE_HAIRSTYLE),
            Product("Dreadlocks", "R400", Product.TYPE_HAIRSTYLE),
            Product("Cornrows", "R250", Product.TYPE_HAIRSTYLE),
            Product("Box Braids", "R500", Product.TYPE_HAIRSTYLE),
            Product("Faux Locs", "R600", Product.TYPE_HAIRSTYLE),
            Product("Twists", "R350", Product.TYPE_HAIRSTYLE)
        )

        // CORRECTED: We now provide the click logic for hairstyles
        val adapter = HomeItemAdapter(dummyHairstyles) { clickedHairstyle ->
            // This now navigates to the HairstyleDetailFragment
            val action = ShopFragmentDirections.actionShopFragmentToHairstyleDetailFragment(clickedHairstyle)
            findNavController().navigate(action)
        }

        binding.gridRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}