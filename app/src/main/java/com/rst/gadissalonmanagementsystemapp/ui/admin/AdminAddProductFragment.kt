package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminAddProductBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AdminAddProductFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminAddProductBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    // --- Activity Result Launchers for permissions, gallery, and camera ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { takeImage() } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for selecting an image from the gallery
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.productImagePreview.setImageURI(it)
        }
    }

    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.productImagePreview.setImageURI(it)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Updated click listener to show the bottom sheet
        binding.productImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "ImagePicker")
        }
        binding.addVariantButton.setOnClickListener {
            addVariantInputRow()
        }
        binding.saveProductButton.setOnClickListener {
            saveProduct()
        }

        addVariantInputRow()
    }

    // This function is called when an option is selected in the bottom sheet
    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                binding.productImagePreview.setImageResource(R.drawable.ic_add_a_photo)
                selectedImageUri = null
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takeImage()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takeImageLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.provider", tmpFile)
    }

    private fun addVariantInputRow() {
        val inflater = LayoutInflater.from(context)
        val variantView = inflater.inflate(R.layout.item_admin_variant_input, binding.variantsContainer, false)
        binding.variantsContainer.addView(variantView)
    }

    private fun saveProduct() {
        val name = binding.nameInput.text.toString().trim()
        val imageUri = selectedImageUri

        if (name.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Product name and image are required", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val variants = mutableListOf<ProductVariant>()
        for (i in 0 until binding.variantsContainer.childCount) {
            val variantView = binding.variantsContainer.getChildAt(i)
            val size = variantView.findViewById<EditText>(R.id.size_input).text.toString().trim()
            val price = variantView.findViewById<EditText>(R.id.price_input).text.toString()
                .toDoubleOrNull()
            val stock =
                variantView.findViewById<EditText>(R.id.stock_input).text.toString().toIntOrNull()

            if (size.isNotEmpty() && price != null && stock != null) {
                variants.add(ProductVariant(size, price, stock = stock))
            }
        }

        if (variants.isEmpty()) {
            Toast.makeText(context, "Please add at least one valid variant", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Show a loading indicator
        // binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveProductButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val productId = "prod_${UUID.randomUUID()}"

            // First, upload the image to Firebase Storage
            val imageUploadResult =
                FirebaseManager.uploadImage(selectedImageUri!!, "products", "$productId.jpg")

            if (imageUploadResult.isSuccess) {
                val imageUrl = imageUploadResult.getOrNull().toString()

                // Then, create the product object with the image URL
                val newProduct = Product(
                    id = productId,
                    name = name,
                    variants = variants,
                    imageUrl = imageUrl // Store the URL from Firebase Storage
                )

                // Finally, save the product data to Firestore
                val addProductResult = FirebaseManager.addProduct(newProduct)
                if (addProductResult.isSuccess) {
                    Toast.makeText(context, "$name added successfully", Toast.LENGTH_SHORT).show()
                    // On success, we navigate away. The button does not need to be re-enabled.
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        context,
                        "Error saving product: ${addProductResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    // FIX: Only re-enable the button if something fails.
                    binding.saveProductButton.isEnabled = true
                }

            } else {
                Toast.makeText(
                    context,
                    "Error uploading image: ${imageUploadResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                // FIX: Only re-enable the button if something fails.
                binding.saveProductButton.isEnabled = true
            }
            // binding.loadingIndicator.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}