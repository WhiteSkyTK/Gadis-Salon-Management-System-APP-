package com.rst.gadissalonmanagementsystemapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProductDetailBinding

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()

    // Get the SAME ViewModel instance that the Activity is using
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val product = args.product

        // --- 1. Populate Views ---
        binding.productImage.setImageResource(product.imageResId)
        binding.productNameDetail.text = product.name
        binding.productPriceNew.text = product.detail
        binding.productPriceOld.visibility = View.GONE

        // --- 2. Setup Favorites Logic ---
        mainViewModel.setCurrentProduct(product)

        // --- 3. Setup Add to Cart Logic ---
        binding.addToCartButton.setOnClickListener {
            AppData.addToCart(product)
            Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}