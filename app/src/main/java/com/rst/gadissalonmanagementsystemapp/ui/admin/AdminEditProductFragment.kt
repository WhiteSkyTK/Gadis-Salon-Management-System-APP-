package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.Product
import com.rst.gadissalonmanagementsystemapp.ProductVariant
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditProductBinding
import kotlinx.coroutines.launch

class AdminEditProductFragment : Fragment() {

    private var _binding: FragmentAdminEditProductBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditProductFragmentArgs by navArgs()
    private lateinit var productToEdit: Product

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productToEdit = args.product

        populateFields()

        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }

        binding.addVariantButton.setOnClickListener {
            addVariantInputRow(null) // Pass null for a new, empty row
        }
    }

    private fun populateFields() {
        binding.productImagePreview.load(productToEdit.imageUrl) {
            placeholder(R.drawable.ic_placeholder_image)
        }
        binding.nameInput.setText(productToEdit.name)

        // Clear any default rows and add rows for each existing variant
        binding.variantsContainer.removeAllViews()
        productToEdit.variants.forEach { variant ->
            addVariantInputRow(variant)
        }
    }

    private fun addVariantInputRow(variant: ProductVariant?) {
        val inflater = LayoutInflater.from(context)
        val variantView = inflater.inflate(R.layout.item_admin_variant_input, binding.variantsContainer, false)

        // If a variant was passed, pre-fill its data
        if (variant != null) {
            variantView.findViewById<EditText>(R.id.size_input).setText(variant.size)
            variantView.findViewById<EditText>(R.id.price_input).setText(variant.price.toString())
            variantView.findViewById<EditText>(R.id.stock_input).setText(variant.stock.toString())
        }

        binding.variantsContainer.addView(variantView)
    }

    private fun saveChanges() {
        val newName = binding.nameInput.text.toString().trim()
        val newVariants = mutableListOf<ProductVariant>()

        // Loop through all the variant rows in the layout
        for (i in 0 until binding.variantsContainer.childCount) {
            val variantView = binding.variantsContainer.getChildAt(i)
            val size = variantView.findViewById<EditText>(R.id.size_input).text.toString().trim()
            val price = variantView.findViewById<EditText>(R.id.price_input).text.toString().toDoubleOrNull()
            val stock = variantView.findViewById<EditText>(R.id.stock_input).text.toString().toIntOrNull()

            if (size.isNotEmpty() && price != null && stock != null) {
                newVariants.add(ProductVariant(size, price, stock = stock))
            }
        }

        if (newName.isEmpty() || newVariants.isEmpty()) {
            Toast.makeText(context, "Name and at least one valid variant are required.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the updated product object, keeping the original ID and image URL
        val updatedProduct = productToEdit.copy(
            name = newName,
            variants = newVariants
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateProduct(updatedProduct)
            if (result.isSuccess) {
                Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}