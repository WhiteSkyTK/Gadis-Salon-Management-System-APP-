package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddProductBinding
import java.util.UUID

class AdminAddProductFragment : Fragment() {

    private var _binding: FragmentAdminAddProductBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addVariantButton.setOnClickListener {
            addVariantInputRow()
        }

        binding.saveProductButton.setOnClickListener {
            saveProduct()
        }

        // Add one variant row by default
        addVariantInputRow()
    }

    private fun addVariantInputRow() {
        val inflater = LayoutInflater.from(context)
        val variantView = inflater.inflate(R.layout.item_admin_variant_input, binding.variantsContainer, false)
        binding.variantsContainer.addView(variantView)
    }

    private fun saveProduct() {
        val name = binding.nameInput.text.toString().trim()
        val reviews = binding.reviewsInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Product name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val variants = mutableListOf<ProductVariant>()
        for (i in 0 until binding.variantsContainer.childCount) {
            val variantView = binding.variantsContainer.getChildAt(i)
            val size = variantView.findViewById<EditText>(R.id.size_input).text.toString().trim()
            val price = variantView.findViewById<EditText>(R.id.price_input).text.toString().toDoubleOrNull()
            val stock = variantView.findViewById<EditText>(R.id.stock_input).text.toString().toIntOrNull()

            if (size.isNotEmpty() && price != null && stock != null) {
                variants.add(ProductVariant(size, price, stock = stock))
            }
        }

        if (variants.isEmpty()) {
            Toast.makeText(context, "Please add at least one valid variant", Toast.LENGTH_SHORT).show()
            return
        }

        val newProduct = Product(
            id = "prod_${UUID.randomUUID()}",
            name = name,
            reviews = reviews,
            variants = variants
        )

        AppData.addProduct(newProduct)
        Toast.makeText(context, "$name added successfully", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack() // Go back to the previous screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}