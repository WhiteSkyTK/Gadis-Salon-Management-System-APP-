package com.rst.gadissalonmanagementsystemapp.ui

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProductDetailBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var selectedVariant: ProductVariant? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Get the arguments passed from the previous screen ---
        val product = args.product
        val role = args.userRole

        // --- 2. Tell the ViewModel about the current product for the favorite icon ---
        mainViewModel.setCurrentProduct(product)

        // --- 3. Populate the static views with product data ---
        binding.productImage.load(product.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_placeholder_image)
        }
        binding.productNameDetail.text = product.name
        // You can populate the reviews text here if you add it back to the Product data class
        // binding.productReviews.text = product.reviews

        // --- 4. Dynamically create the size chips ---
        setupSizeChips(product)

        // --- 5. THE KEY FIX: Show or hide the bottom bar based on the user's role ---
        if (role.equals("WORKER", ignoreCase = true)) {
            binding.bottomBar.visibility = View.GONE
        } else {
            binding.bottomBar.visibility = View.VISIBLE
        }

        // --- 6. Setup click listeners (these will only be visible for customers) ---
        binding.addToCartButton.setOnClickListener {
            if (selectedVariant == null) {
                Toast.makeText(context, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val result = FirebaseManager.addToCart(product, selectedVariant!!)
                if (result.isSuccess) {
                    Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buyNowButton.setOnClickListener {
            if (selectedVariant == null) {
                Toast.makeText(context, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val action = ProductDetailFragmentDirections.actionProductDetailFragmentToPurchaseConfirmationFragment(product)
            findNavController().navigate(action)
        }
    }

    private fun setupSizeChips(product: Product) {
        val chipGroup = binding.sizeChipGroup
        chipGroup.removeAllViews() // Clear any old chips

        product.variants.forEach { variant ->
            val chip = layoutInflater.inflate(R.layout.chip_stylist, chipGroup, false) as Chip
            chip.text = variant.size
            chip.tag = variant // Store the full variant object in the chip's tag
            chipGroup.addView(chip)
        }

        // --- 4. Handle Chip Selection ---
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            if (selectedChip != null) {
                val selectedVariant = selectedChip.tag as ProductVariant
                updatePrice(selectedVariant)
            }
        }

        // --- 5. Select the first chip by default ---
        (chipGroup.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun updatePrice(variant: ProductVariant) {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.productPriceNew.text = format.format(variant.price)

        if (variant.priceOld != null) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = format.format(variant.priceOld)
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}