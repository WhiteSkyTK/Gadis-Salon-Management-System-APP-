package com.rst.gadissalonmanagementsystemapp.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.CartAdapter
import com.rst.gadissalonmanagementsystemapp.CartItem
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AppData.currentUserCart.observe(viewLifecycleOwner) { cartItems ->
            binding.cartRecyclerView.adapter = CartAdapter(cartItems)

            // Recalculate and display the total price whenever the cart changes
            val totalPrice = cartItems.sumOf { it.price * it.quantity }
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.totalPrice.text = format.format(totalPrice)
        }

        binding.buyNowButtonCart.setOnClickListener {
            // In a real app, you would process the payment here
            Toast.makeText(context, "Purchase Complete!", Toast.LENGTH_LONG).show()
            // Navigate to the new success screen
            findNavController().navigate(R.id.action_cartFragment_to_purchaseConfirmationFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}