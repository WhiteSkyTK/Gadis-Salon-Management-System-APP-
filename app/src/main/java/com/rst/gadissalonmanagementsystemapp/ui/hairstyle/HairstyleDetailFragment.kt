package com.rst.gadissalonmanagementsystemapp.ui.hairstyle

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHairstyleDetailBinding
import com.rst.gadissalonmanagementsystemapp.MainViewModel
import com.rst.gadissalonmanagementsystemapp.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

class HairstyleDetailFragment : Fragment() {

    private var _binding: FragmentHairstyleDetailBinding? = null
    private val binding get() = _binding!!

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
        mainViewModel.setCurrentFavoritableItem(hairstyle)

        // Use the new Hairstyle data to populate the views
        binding.hairstyleImage.load(hairstyle.imageUrl)
        binding.hairstyleNameDetail.text = hairstyle.name
        binding.hairstyleDescription.text = hairstyle.description
        binding.hairstyleDuration.text = "Duration: ${hairstyle.durationHours} hours"

        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val originalPrice = hairstyle.price

        // Calculate a random "old" price that is 30% to 70% higher
        val priceIncreasePercentage = Random.nextDouble(0.30, 0.71)
        val oldPrice = originalPrice * (1 + priceIncreasePercentage)

        // Set the actual price
        binding.hairstylePrice.text = format.format(originalPrice)

        // Set the "old" price, apply the strikethrough, and make it visible
        binding.hairstylePriceOld.text = format.format(oldPrice)
        binding.hairstylePriceOld.paintFlags = binding.hairstylePriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        binding.hairstylePriceOld.visibility = View.VISIBLE


        binding.bookNowButton.setOnClickListener {
            // This correctly navigates to the confirmation screen
            val action = HairstyleDetailFragmentDirections.actionHairstyleDetailFragmentToBookingConfirmationFragment(hairstyle)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}