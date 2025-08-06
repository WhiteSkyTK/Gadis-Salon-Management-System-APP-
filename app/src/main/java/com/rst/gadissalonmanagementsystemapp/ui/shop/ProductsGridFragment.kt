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

class ProductsGridFragment : Fragment() {

    private var _binding: FragmentItemGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dummyProducts = listOf(
            Product("Eco Style Gel", "R50", Product.TYPE_PRODUCT),
            Product("Shampoo", "R80", Product.TYPE_PRODUCT),
            Product("Conditioner", "R85", Product.TYPE_PRODUCT),
            Product("Hair Spray", "R120", Product.TYPE_PRODUCT),
            Product("Leave-in Treatment", "R150", Product.TYPE_PRODUCT),
            Product("Hair Food", "R65", Product.TYPE_PRODUCT)
        )

        // CORRECTED: We now pass the click listener lambda as the second argument
        val adapter = HomeItemAdapter(dummyProducts) { clickedProduct ->
            // This code runs when a product in the grid is clicked
            val action = ShopFragmentDirections.actionShopFragmentToProductDetailFragment(clickedProduct)
            findNavController().navigate(action)
        }

        binding.gridRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}