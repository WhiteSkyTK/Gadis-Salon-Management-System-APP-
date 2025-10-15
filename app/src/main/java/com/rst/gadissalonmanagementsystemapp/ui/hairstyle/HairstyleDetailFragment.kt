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
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
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
        // Setup is now handled in onStart
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            setupUI()
        } else {
            showOfflineUI(true)
        }
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupUI() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        val hairstyle = args.hairstyle
        mainViewModel.setCurrentFavoritableItem(hairstyle)

        binding.hairstyleImage.load(hairstyle.imageUrl)
        binding.hairstyleNameDetail.text = hairstyle.name
        binding.hairstyleDescription.text = hairstyle.description
        binding.hairstyleDuration.text = "Duration: ${hairstyle.durationHours} hours"

        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val originalPrice = hairstyle.price
        val priceIncreasePercentage = Random.nextDouble(0.30, 0.71)
        val oldPrice = originalPrice * (1 + priceIncreasePercentage)

        binding.hairstylePrice.text = format.format(originalPrice)
        binding.hairstylePriceOld.text = format.format(oldPrice)
        binding.hairstylePriceOld.paintFlags = binding.hairstylePriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        binding.hairstylePriceOld.visibility = View.VISIBLE

        binding.bookNowButton.setOnClickListener {
            val action = HairstyleDetailFragmentDirections.actionHairstyleDetailFragmentToBookingConfirmationFragment(hairstyle)
            findNavController().navigate(action)
        }

        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
