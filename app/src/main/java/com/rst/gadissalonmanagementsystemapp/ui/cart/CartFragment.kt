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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartAdapter: CartAdapter

    private var cartListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.buyNowButtonCart.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_purchaseConfirmationFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for cart updates when the screen becomes visible
        listenForCartUpdates()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the screen is no longer visible to prevent crashes
        cartListener?.remove()
    }


    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = mutableListOf(),
            onQuantityChanged = { cartItem, newQuantity ->
                // This now calls our new "smart" function with stock checking
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = FirebaseManager.updateCartItemQuantity(cartItem.productId, cartItem.size, newQuantity)
                    if (!result.isSuccess) {
                        Toast.makeText(context, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRemove = { cartItem ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val cartItemId = "${cartItem.productId}_${cartItem.size}"
                    FirebaseManager.removeCartItem(cartItemId)
                }
            }
        )
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.cartRecyclerView.adapter = cartAdapter
    }

    private fun listenForCartUpdates() {
        FirebaseManager.addCurrentUserCartListener { cartItems ->
            if (view != null) {
                if (cartItems.isEmpty()) {
                    binding.cartRecyclerView.visibility = View.GONE
                    binding.emptyCartText.visibility = View.VISIBLE
                    binding.summaryCard.visibility = View.GONE // Hide bottom bar
                } else {
                    binding.cartRecyclerView.visibility = View.VISIBLE
                    binding.emptyCartText.visibility = View.GONE
                    binding.summaryCard.visibility = View.VISIBLE // Show bottom bar
                    cartAdapter.updateData(cartItems)
                }

                // Always update the total price
                val totalPrice = cartItems.sumOf { it.price * it.quantity }
                val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                binding.totalPrice.text = format.format(totalPrice)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}