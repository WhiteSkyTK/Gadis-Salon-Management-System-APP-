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
        val product = args.product
        val role = args.userRole

        mainViewModel.setCurrentFavoritableItem(product)

        // --- Populate Static Views ---
        binding.productImage.load(product.imageUrl)
        binding.productNameDetail.text = product.name
        binding.productReviews.text = product.reviews

        // --- Setup Dynamic UI based on Role ---
        if (role.equals("WORKER", ignoreCase = true)) {
            binding.bottomBar.visibility = View.GONE
            binding.stockInfoCard.visibility = View.VISIBLE
        } else {
            binding.bottomBar.visibility = View.VISIBLE
            binding.stockInfoCard.visibility = View.GONE
        }

        setupSizeChips(product)
        setupClickListeners(product)
    }

    private fun setupSizeChips(product: Product) {
        val chipGroup = binding.sizeChipGroup
        chipGroup.removeAllViews()

        if (product.variants.isEmpty()) {
            // Handle case where a product might have no variants
            binding.bottomBar.visibility = View.GONE // Hide buy/cart buttons
            return
        }

        product.variants.forEach { variant ->
            val chip = layoutInflater.inflate(R.layout.chip_stylist, chipGroup, false) as Chip
            chip.text = variant.size
            chip.tag = variant
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            if (selectedChip != null) {
                selectedVariant = selectedChip.tag as ProductVariant
                updateDynamicInfo(selectedVariant!!)
            }
        }

        // 1. Programmatically find the first chip.
        (chipGroup.getChildAt(0) as? Chip)?.let { firstChip ->
            // 2. Set its checked state to true. This will now trigger the listener above.
            firstChip.isChecked = true
        }
    }

    private fun updateDynamicInfo(variant: ProductVariant) {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.productPriceNew.text = format.format(variant.price)

        // Update old price if it exists
        if (variant.priceOld != null) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = format.format(variant.priceOld)
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.GONE
        }

        // Update the stock count text
        binding.stockCountText.text = "Stock Remaining: ${variant.stock}"
    }

    private fun setupClickListeners(product: Product) {
        binding.addToCartButton.setOnClickListener {
            if (selectedVariant == null) {
                Toast.makeText(context, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                // --- THIS IS THE FIX: The addToCart function now handles the composite ID internally ---
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

            val action = ProductDetailFragmentDirections.actionProductDetailFragmentToPurchaseConfirmationFragment(
                selectedVariant = selectedVariant!!,
                product = product
            )
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}