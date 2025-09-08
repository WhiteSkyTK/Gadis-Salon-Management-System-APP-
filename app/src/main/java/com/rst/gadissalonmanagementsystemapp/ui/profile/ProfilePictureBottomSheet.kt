package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rst.gadissalonmanagementsystemapp.databinding.BottomSheetProfilePictureBinding

class ProfilePictureBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetProfilePictureBinding? = null
    private val binding get() = _binding!!
    private var listener: PictureOptionListener? = null

    // Interface to communicate back to the Fragment
    interface PictureOptionListener {
        fun onOptionSelected(option: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Attach the listener from the parent fragment
        listener = parentFragment as? PictureOptionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetProfilePictureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.optionGallery.setOnClickListener {
            listener?.onOptionSelected("gallery")
            dismiss()
        }
        binding.optionCamera.setOnClickListener {
            listener?.onOptionSelected("camera")
            dismiss()
        }
        binding.optionRemove.setOnClickListener {
            listener?.onOptionSelected("remove")
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}