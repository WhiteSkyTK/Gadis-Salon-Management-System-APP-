package com.rst.gadissalonmanagementsystemapp.ui.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.CartAdapter
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentPurchaseConfirmationBinding

class PurchaseConfirmationFragment : Fragment() {

    private var _binding: FragmentPurchaseConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args: PurchaseConfirmationFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPurchaseConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if a single product was passed from the detail screen
        val productToShow = args.product

        if (productToShow != null) {
            // A single product was passed. Show only that item.
            val price = productToShow.variants.firstOrNull()?.price ?: 0.0
            val singleItemInCart = listOf(CartItem(productToShow.name, price, 1, productToShow.imageResId))
            binding.summaryRecyclerView.adapter = CartAdapter(singleItemInCart)
        } else {
            // No product was passed, so show the entire cart from AppData
            AppData.currentUserCart.observe(viewLifecycleOwner) { cartItems ->
                binding.summaryRecyclerView.adapter = CartAdapter(cartItems)
            }
        }

        binding.confirmPurchaseButton.setOnClickListener {
            // In a real app, you would process the payment here
            Toast.makeText(context, "Purchase Complete!", Toast.LENGTH_LONG).show()
            // Navigate to the success screen
            findNavController().navigate(R.id.purchaseSuccessFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}