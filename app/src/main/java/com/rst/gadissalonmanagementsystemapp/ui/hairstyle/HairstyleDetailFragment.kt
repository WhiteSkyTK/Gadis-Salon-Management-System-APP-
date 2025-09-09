package com.rst.gadissalonmanagementsystemapp.ui.hairstyle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rst.gadissalonmanagementsystemapp.AppData
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHairstyleDetailBinding
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.Product
import java.text.NumberFormat
import java.util.Locale

class HairstyleDetailFragment : Fragment() {

    private var _binding: FragmentHairstyleDetailBinding? = null
    private val binding get() = _binding!!

    // Use Safe Args to get the passed arguments
    private val args: HairstyleDetailFragmentArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHairstyleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the hairstyle object from the arguments
        val hairstyle = args.hairstyle // We are reusing the 'Product' data class

        // Now we need to pass a temporary Product to the ViewModel for the favorite logic to work
        val tempProductForFavorite = Product(hairstyle.id, hairstyle.name, "", listOf())
        mainViewModel.setCurrentProduct(tempProductForFavorite)

        // Use the new Hairstyle data to populate the views
        binding.hairstyleImage.setImageResource(hairstyle.imageResId)
        binding.hairstyleNameDetail.text = hairstyle.name
        binding.hairstyleDescription.text = hairstyle.description
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        binding.hairstylePrice.text = format.format(hairstyle.price)
        binding.hairstyleDuration.text = "Duration: ${hairstyle.durationHours} hours"

        // Find the stylist's name from AppData
        val stylist = AppData.allStylists.find { it.id == hairstyle.availableStylistIds.firstOrNull() }
        binding.stylistName.text = "Stylist: ${stylist?.name ?: "Any"}"

        binding.bookNowButton.setOnClickListener {
            // Create the navigation action, passing the current hairstyle object
            val action = HairstyleDetailFragmentDirections.actionHairstyleDetailFragmentToBookingConfirmationFragment(hairstyle)

            // Navigate to the confirmation screen
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}