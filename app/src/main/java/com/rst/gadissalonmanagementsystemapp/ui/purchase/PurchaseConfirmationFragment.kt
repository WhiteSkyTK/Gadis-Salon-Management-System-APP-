package com.rst.gadissalonmanagementsystemapp.ui.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentPurchaseConfirmationBinding
import com.rst.gadissalonmanagementsystemapp.ui.orders.OrderDetailAdapter
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.util.UUID

class PurchaseConfirmationFragment : Fragment() {

    private var _binding: FragmentPurchaseConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: PurchaseConfirmationFragmentArgs by navArgs()
    private var itemsToPurchase = listOf<CartItem>()

    private var cartListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPurchaseConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.summaryRecyclerView.layoutManager = LinearLayoutManager(context)

        binding.confirmPurchaseButton.setOnClickListener {
            confirmPurchase()
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            loadConfirmationData()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        cartListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun loadConfirmationData() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        val product = args.product
        val variant = args.selectedVariant

        if (product != null && variant != null) {
            // Case 1: "Buy Now" - data is passed directly
            itemsToPurchase = listOf(CartItem(
                productId = product.id, name = product.name, size = variant.size,
                price = variant.price, quantity = 1, imageUrl = product.imageUrl
            ))
            binding.summaryRecyclerView.adapter = OrderDetailAdapter(itemsToPurchase)
            // Stop shimmer and show content immediately
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE
        } else {
            // Case 2: "Checkout from Cart" - data is loaded from Firebase
            cartListener = FirebaseManager.addCurrentUserCartListener { cartItems ->
                if (view != null) {
                    itemsToPurchase = cartItems
                    binding.summaryRecyclerView.adapter = OrderDetailAdapter(itemsToPurchase)
                    // Stop shimmer and show content after data is loaded
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.contentContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun confirmPurchase() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userResult = FirebaseManager.getCurrentUser()
            if (!userResult.isSuccess || userResult.getOrNull() == null) {
                Toast.makeText(context, "You must be logged in to make a purchase.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val currentUser = userResult.getOrNull()!! // We can now safely use the user object

            if (itemsToPurchase.isEmpty()) {
                Toast.makeText(context, "Your cart is empty.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val total = itemsToPurchase.sumOf { it.price * it.quantity }
            val order = ProductOrder(
                id = UUID.randomUUID().toString(),
                customerId = currentUser.id,
                customerName = currentUser.name, // The error is now fixed
                items = itemsToPurchase,
                totalPrice = total
            )

            val result = FirebaseManager.createProductOrder(order)
            if (result.isSuccess) {
                // Navigate to the success screen, clearing the back stack
                findNavController().navigate(R.id.action_purchaseConfirmationFragment_to_purchaseSuccessFragment)
            } else {
                Toast.makeText(context, "Purchase failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}