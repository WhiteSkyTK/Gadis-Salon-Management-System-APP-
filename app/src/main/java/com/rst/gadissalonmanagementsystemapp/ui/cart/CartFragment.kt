package com.rst.gadissalonmanagementsystemapp.ui.cart

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCartBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartAdapter: CartAdapter
    private var cartListener: ListenerRegistration? = null
    private var localCartItems: MutableList<CartItem> = mutableListOf()
    private var hasCartChanged: Boolean = false
    private var isListenerActive: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.buyNowButtonCart.setOnClickListener {
            // --- NEW: Save cart before navigating ---
            saveCartToFirestoreIfNeeded(true) // Force save now
            // --- END NEW ---

            // Filter out sold-out items before checkout
            val itemsToPurchase = localCartItems.filter { it.stock > 0 && it.quantity > 0 }

            if (itemsToPurchase.isEmpty()) {
                Toast.makeText(context, "Your cart is empty or all items are sold out.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val action = CartFragmentDirections.actionCartFragmentToPurchaseConfirmationFragment(
                product = null,
                selectedVariant = null,
                cartItems = itemsToPurchase.toTypedArray()
            )
            findNavController().navigate(action)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            isListenerActive = true
            listenForCartUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        isListenerActive = false // Stop listener from overwriting
        cartListener?.remove()
        saveCartToFirestoreIfNeeded(false) // Save in background
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = localCartItems, // Pass the local list
            onQuantityChanged = { cartItem, newQuantity ->
                // --- THIS IS THE FIX ---
                // This now updates the LOCAL list, not Firebase
                val itemIndex = localCartItems.indexOfFirst { it.productId == cartItem.productId && it.size == cartItem.size }
                if (itemIndex != -1) {
                    localCartItems[itemIndex].quantity = newQuantity
                    hasCartChanged = true
                    cartAdapter.notifyItemChanged(itemIndex) // More efficient update
                    updateTotalPrice()
                }
                // --- END FIX ---
            },
            onRemove = { cartItem ->
                // --- THIS IS THE FIX ---
                // This also updates the LOCAL list
                val itemIndex = localCartItems.indexOfFirst { it.productId == cartItem.productId && it.size == cartItem.size }
                if (itemIndex != -1) {
                    localCartItems.removeAt(itemIndex)
                    hasCartChanged = true
                    cartAdapter.notifyItemRemoved(itemIndex)
                    updateTotalPrice()
                    if(localCartItems.isEmpty()) showEmptyUI()
                }
                // --- END FIX ---
            },
            onStockError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.cartRecyclerView.adapter = cartAdapter
    }

    private fun listenForCartUpdates() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        cartListener?.remove() // Remove old listener
        cartListener = FirebaseManager.addCurrentUserCartListener { cartItemsWithStock ->
            // --- MODIFIED: Only update if listener is active and no local changes ---
            if (view != null && isListenerActive && !hasCartChanged) {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE

                localCartItems.clear()
                localCartItems.addAll(cartItemsWithStock)

                if (cartItemsWithStock.isEmpty()) {
                    showEmptyUI()
                } else {
                    binding.cartRecyclerView.visibility = View.VISIBLE
                    binding.emptyCartText.visibility = View.GONE
                    binding.summaryCard.visibility = View.VISIBLE
                    cartAdapter.updateData(cartItemsWithStock)
                }
                updateTotalPrice()
            } else if (view != null) {
                // Already have local changes, just stop shimmer
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE
                Log.d("CartFragment", "Skipping Firestore update due to local changes.")
            }
        }
    }

    // --- NEW: Helper to update total price ---
    private fun updateTotalPrice() {
        // Filter out sold out items from price calculation
        val validItems = localCartItems.filter { it.stock > 0 }
        val totalPrice = validItems.sumOf { it.price * it.quantity }
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.totalPrice.text = format.format(totalPrice)
        binding.buyNowButtonCart.isEnabled = validItems.isNotEmpty()
    }

    // --- NEW: Helper to show empty UI ---
    private fun showEmptyUI() {
        binding.cartRecyclerView.visibility = View.GONE
        binding.emptyCartText.visibility = View.VISIBLE
        binding.summaryCard.visibility = View.GONE
    }

    // --- NEW: Function to save cart to Firebase ---
    private fun saveCartToFirestoreIfNeeded(isNavigating: Boolean) {
        if (!hasCartChanged) {
            Log.d("CartFragment", "No changes to save.")
            return // No changes to save
        }
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Log.w("CartFragment", "Offline, cannot save cart.")
            if (isNavigating) Toast.makeText(context, "Offline, cart changes not saved.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("CartFragment", "Saving cart to Firestore...")
        // Filter out items with 0 quantity (e.g., if they became sold out)
        val cartToSave = localCartItems.filter { it.quantity > 0 }
        hasCartChanged = false // Reset flag immediately

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.saveCart(cartToSave)
            if (result.isSuccess) {
                Log.d("CartFragment", "Cart saved successfully.")
            } else {
                Log.e("CartFragment", "Failed to save cart: ${result.exceptionOrNull()?.message}")
                // Restore flag so it tries again later
                hasCartChanged = true
                if (isNavigating) {
                    Toast.makeText(context, "Failed to save cart.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

