package com.rst.gadissalonmanagementsystemapp.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.AppData
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

        AppData.allProducts.observe(viewLifecycleOwner) { productList ->
            val adapter = HomeItemAdapter(productList) { clickedProduct ->
                val action = ShopFragmentDirections.actionShopFragmentToProductDetailFragment(clickedProduct)
                findNavController().navigate(action)
            }
            binding.gridRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}