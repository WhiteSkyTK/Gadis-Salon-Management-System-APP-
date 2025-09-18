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
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentPurchaseConfirmationBinding
import com.rst.gadissalonmanagementsystemapp.ui.cart.CartAdapter
import kotlinx.coroutines.launch
import java.util.UUID

class PurchaseConfirmationFragment : Fragment() {

    private var _binding: FragmentPurchaseConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: PurchaseConfirmationFragmentArgs by navArgs()
    private var itemsToPurchase = listOf<CartItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPurchaseConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.summaryRecyclerView.layoutManager = LinearLayoutManager(context)

        val productToShow = args.product
        if (productToShow != null) {
            // Case 1: User clicked "Buy Now" on a single product
            val variant = productToShow.variants.firstOrNull()
            if (variant != null) {
                itemsToPurchase = listOf(CartItem(productToShow.id, productToShow.name, variant.price, 1, productToShow.imageUrl))
                // The adapter for a summary list doesn't need to be interactive
                binding.summaryRecyclerView.adapter = CartAdapter(itemsToPurchase, { _, _ -> }, { })
            }
        } else {
            // Case 2: User is checking out with their full cart
            FirebaseManager.addCurrentUserCartListener { cartItems ->
                itemsToPurchase = cartItems
                binding.summaryRecyclerView.adapter = CartAdapter(itemsToPurchase, { _, _ -> }, { })
            }
        }

        binding.confirmPurchaseButton.setOnClickListener {
            confirmPurchase()
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