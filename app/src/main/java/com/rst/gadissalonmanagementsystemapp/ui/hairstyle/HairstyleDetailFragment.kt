package com.rst.gadissalonmanagementsystemapp.ui.hairstyle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentHairstyleDetailBinding
import com.rst.gadissalonmanagementsystemapp.MainViewModel

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
        val hairstyle = args.product // We are reusing the 'Product' data class

        // Use the data to populate the views
        binding.hairstyleImage.setImageResource(hairstyle.imageResId)
        binding.hairstyleNameDetail.text = hairstyle.name
        binding.hairstylePrice.text = hairstyle.detail

        mainViewModel.setCurrentProduct(hairstyle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}