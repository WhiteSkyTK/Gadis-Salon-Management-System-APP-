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
import androidx.navigation.fragment.navArgs
import coil.load
import com.rst.gadissalonmanagementsystemapp.*
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminEditProductBinding
import com.rst.gadissalonmanagementsystemapp.ui.profile.ProfilePictureBottomSheet
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import java.io.File

class AdminEditProductFragment : Fragment(), ProfilePictureBottomSheet.PictureOptionListener {

    private var _binding: FragmentAdminEditProductBinding? = null
    private val binding get() = _binding!!
    private val args: AdminEditProductFragmentArgs by navArgs()
    private lateinit var productToEdit: Product
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    // --- Activity Result Launchers for image picking ---
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takeImage() else Toast.makeText(context, "Camera permission needed.", Toast.LENGTH_SHORT).show()
    }
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.productImagePreview.setImageURI(it)
        }
    }
    private val takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let {
                selectedImageUri = it
                binding.productImagePreview.setImageURI(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productToEdit = args.product
        populateFields()

        binding.productImagePreview.setOnClickListener {
            ProfilePictureBottomSheet().show(childFragmentManager, "ProductImagePicker")
        }
        binding.saveChangesButton.setOnClickListener { saveChanges() }
        binding.addVariantButton.setOnClickListener { addVariantInputRow(null) }
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            populateFields()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }


    private fun populateFields() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        // This is a quick operation, so shimmer will be brief
        binding.productImagePreview.load(productToEdit.imageUrl.ifEmpty { R.drawable.ic_add_a_photo })
        binding.nameInput.setText(productToEdit.name)
        binding.variantsContainer.removeAllViews()
        productToEdit.variants.forEach { addVariantInputRow(it) }

        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
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

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.saveChangesButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var imageUrl = productToEdit.imageUrl
            if (selectedImageUri != null) {
                val uploadResult = FirebaseManager.uploadImage(selectedImageUri!!, "products", productToEdit.id)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull().toString()
                } else {
                    Toast.makeText(context, "Image upload failed.", Toast.LENGTH_SHORT).show()
                    binding.saveChangesButton.isEnabled = true
                    return@launch
                }
            }

            val updatedProduct = productToEdit.copy(name = newName, variants = newVariants, imageUrl = imageUrl)
            val result = FirebaseManager.updateProduct(updatedProduct)
            if (result.isSuccess) {
                Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.saveChangesButton.isEnabled = false
        }
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "gallery" -> selectImageLauncher.launch("image/*")
            "camera" -> checkCameraPermissionAndTakePhoto()
            "remove" -> {
                selectedImageUri = null
                binding.productImagePreview.setImageResource(R.drawable.ic_add_a_photo)
                productToEdit = productToEdit.copy(imageUrl = "") // Mark for removal on save
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> takeImage()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takeImageLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image", ".png", requireContext().cacheDir).apply { createNewFile(); deleteOnExit() }
        return FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.provider", tmpFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}