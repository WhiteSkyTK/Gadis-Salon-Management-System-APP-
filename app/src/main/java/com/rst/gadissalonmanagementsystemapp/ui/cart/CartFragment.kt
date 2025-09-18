package com.rst.gadissalonmanagementsystemapp.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.ui.cart.CartAdapter
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCartBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForCartUpdates()

        binding.buyNowButtonCart.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_purchaseConfirmationFragment)
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = mutableListOf(),
            onQuantityChanged = { productId, newQuantity ->
                lifecycleScope.launch { FirebaseManager.updateCartItemQuantity(productId, newQuantity) }
            },
            onRemove = { productId ->
                lifecycleScope.launch { FirebaseManager.removeCartItem(productId) }
            }
        )
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.cartRecyclerView.adapter = cartAdapter
    }

    private fun listenForCartUpdates() {
        FirebaseManager.addCurrentUserCartListener { cartItems ->
            cartAdapter.updateData(cartItems)
            val totalPrice = cartItems.sumOf { it.price * it.quantity }
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.totalPrice.text = format.format(totalPrice)
        }
    }
}