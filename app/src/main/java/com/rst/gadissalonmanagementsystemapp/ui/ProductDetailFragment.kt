package com.rst.gadissalonmanagementsystemapp.ui

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.chip.Chip
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentProductDetailBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var selectedVariant: ProductVariant? = null
    private val TAG = "ProductDetailFragment" // --- NEW: Added for logging ---


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup is now handled in onStart
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            setupUI()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupUI() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        val product = args.product
        val role = args.userRole

        mainViewModel.setCurrentFavoritableItem(product)
        mainViewModel.setFavoriteState(false)

        mainViewModel.isCurrentItemFavorite.observe(viewLifecycleOwner) { isFavorite ->
            // This fragment doesn't have its own favorite button,
            // this logic is handled by the MainActivity.
            // We just need to make sure the ViewModel has the right data.
        }

        binding.productImage.load(product.imageUrl)
        binding.productNameDetail.text = product.name
        binding.productReviews.text = product.reviews

        if (role.equals("WORKER", ignoreCase = true)) {
            binding.bottomBar.visibility = View.GONE
            binding.stockInfoCard.visibility = View.VISIBLE
        } else {
            binding.bottomBar.visibility = View.VISIBLE
            binding.stockInfoCard.visibility = View.GONE
        }

        setupSizeChips(product)
        setupClickListeners(product)

        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
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
            chip.tag = variant

            // --- NEW: Sold Out Logic ---
            if (variant.stock <= 0) {
                chip.text = "${variant.size} (Sold Out)"
                chip.isEnabled = false
                chip.isChipIconVisible = false
            } else {
                chip.text = variant.size
                chip.isEnabled = true
            }
            // --- END NEW ---

            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            if (selectedChip != null) {
                selectedVariant = selectedChip.tag as ProductVariant
                updateDynamicInfo(selectedVariant!!)

                // --- MODIFIED: Tell the ViewModel which variant is selected ---
                mainViewModel.setSelectedVariant(selectedVariant)
                // --- Now, check if this variant is a favorite ---
                checkIfFavorite(product.id, selectedVariant!!.size)
                // --- END MODIFICATION ---
            }
        }

        // Programmatically find the first available chip
        val firstAvailableChip = chipGroup.children.find { it is Chip && it.isEnabled } as? Chip
        if (firstAvailableChip != null) {
            firstAvailableChip.isChecked = true
        } else {
            // All variants are sold out
            val firstVariant = product.variants.first()
            updateDynamicInfo(firstVariant)
            // --- NEW: Check favorite status for the first variant ---
            mainViewModel.setSelectedVariant(firstVariant) // Tell VM about this variant
            checkIfFavorite(product.id, firstVariant.size)
        }
    }

    private fun checkIfFavorite(productId: String, variantSize: String) {
        // Create the unique ID matching the website format
        val favoriteId = "${productId}_${variantSize}"
        Log.d(TAG, "Checking favorite status for: $favoriteId")

        // --- MODIFIED: Use viewLifecycleOwner.lifecycleScope ---
        // This fixes the 'Unresolved reference viewModelScope' error
        viewLifecycleOwner.lifecycleScope.launch {
            // Use the modified isFavorite function
            val result = FirebaseManager.isFavorite(favoriteId)
            if (result.isSuccess) {
                // Update the ViewModel, which will trigger the observer in MainActivity
                mainViewModel.setFavoriteState(result.getOrDefault(false))
            }
        }
    }

    private fun updateDynamicInfo(variant: ProductVariant) {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.productPriceNew.text = format.format(variant.price)

        // --- MODIFIED: Check for valid priceOld ---
        if (variant.priceOld != null && variant.priceOld > 0) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = format.format(variant.priceOld)
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.GONE
        }

        // --- MODIFIED: Stock & Button State Logic ---
        if (args.userRole.equals("WORKER", ignoreCase = true)) {
            // Worker view (just show stock)
            binding.stockCountText.text = "Stock Remaining: ${variant.stock}"
            binding.bottomBar.visibility = View.GONE
        } else {
            // Customer view (show stock and update buttons)
            if (variant.stock <= 0) {
                binding.stockCountText.text = "Stock: Sold Out"
                binding.stockCountText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
                binding.addToCartButton.isEnabled = false
                binding.buyNowButton.isEnabled = false
                binding.buyNowButton.text = "Sold Out"
            } else {
                // --- NEW: Check if stock is <= 5 ---
                if(variant.stock <= 5) {
                    binding.stockCountText.text = "Only ${variant.stock} left!"
                    binding.stockCountText.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                } else {
                    binding.stockCountText.text = "Stock: ${variant.stock} Available"
                    binding.stockCountText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green_text))
                }
                // --- END NEW ---
                binding.addToCartButton.isEnabled = true
                binding.buyNowButton.isEnabled = true
                binding.buyNowButton.text = "Buy Now"
            }
        }
        // --- END MODIFICATION ---
    }

    private fun setupClickListeners(product: Product) {
        binding.addToCartButton.setOnClickListener {
            if (selectedVariant == null) {
                Toast.makeText(context, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedVariant!!.stock <= 0) {
                Toast.makeText(context, "This item is sold out", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                // Create a CartItem object to be added
                val cartItem = CartItem(
                    productId = product.id,
                    name = product.name,
                    size = selectedVariant!!.size,
                    price = selectedVariant!!.price,
                    quantity = 1, // We add one at a time from this screen
                    imageUrl = product.imageUrl,
                    stock = selectedVariant!!.stock
                )

                // Call the correct function that works with the cart array
                val result = FirebaseManager.addOrUpdateCartItem(cartItem)
                if (result.isSuccess) {
                    Toast.makeText(context, "${cartItem.name} added to cart", Toast.LENGTH_SHORT).show()
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

            if (selectedVariant!!.stock <= 0) {
                Toast.makeText(context, "This item is sold out", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Create a bundle for the arguments
            val bundle = Bundle().apply {
                putParcelable("selectedVariant", selectedVariant!!)
                putParcelable("product", product)
                // cartItems will be null by default, which is correct
            }

            // 2. Navigate using the action ID from the XML
            findNavController().navigate(R.id.action_productDetailFragment_to_purchaseConfirmationFragment, bundle)
            // --- END WORKAROUND ---
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}