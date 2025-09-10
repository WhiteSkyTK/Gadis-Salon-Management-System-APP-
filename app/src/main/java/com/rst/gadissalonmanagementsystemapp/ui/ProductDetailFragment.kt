package com.rst.gadissalonmanagementsystemapp.ui

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProductDetailBinding
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val product = args.product

        // --- 1. Populate Static Views ---
        binding.productImage.load(product.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_placeholder_image)
        }
        binding.productNameDetail.text = product.name
        // This field is back in the Product class


        // Tell the ViewModel which product we are viewing for the favorite logic
        mainViewModel.setCurrentProduct(product)

        // --- 2. Dynamically Create Size Chips ---
        setupSizeChips(product)

        // --- 3. Setup Add to Cart Logic ---
        binding.addToCartButton.setOnClickListener {
            AppData.addToCart(product)
            Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
        }

        binding.buyNowButton.setOnClickListener {
            // Find the ID of the currently checked chip in the group
            val checkedChipId = binding.sizeChipGroup.checkedChipId
            if (checkedChipId == View.NO_ID) {
                // If no size is selected, show an error
                Toast.makeText(context, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to the confirmation screen, passing the selected product
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