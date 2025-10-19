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
    private var currentCartItems: List<CartItem> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.buyNowButtonCart.setOnClickListener {
            // Pass the current cart items to the checkout/confirmation fragment
            val action = CartFragmentDirections.actionCartFragmentToPurchaseConfirmationFragment(
                product = null,
                selectedVariant = null,
                cartItems = currentCartItems.toTypedArray()
            )
            findNavController().navigate(action)
        }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            listenForCartUpdates()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        cartListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
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
                    FirebaseManager.removeCartItem(cartItem.productId, cartItem.size)
                }
            }
        )
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.cartRecyclerView.adapter = cartAdapter
    }

    private fun listenForCartUpdates() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        cartListener = FirebaseManager.addCurrentUserCartListener { cartItems ->
            if (view != null) {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE

                currentCartItems = cartItems

                if (cartItems.isEmpty()) {
                    binding.cartRecyclerView.visibility = View.GONE
                    binding.emptyCartText.visibility = View.VISIBLE
                    binding.summaryCard.visibility = View.GONE
                } else {
                    binding.cartRecyclerView.visibility = View.VISIBLE
                    binding.emptyCartText.visibility = View.GONE
                    binding.summaryCard.visibility = View.VISIBLE
                    cartAdapter.updateData(cartItems)
                }

                val totalPrice = cartItems.sumOf { it.price * it.quantity }
                val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                binding.totalPrice.text = format.format(totalPrice)
                binding.buyNowButtonCart.isEnabled = cartItems.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

