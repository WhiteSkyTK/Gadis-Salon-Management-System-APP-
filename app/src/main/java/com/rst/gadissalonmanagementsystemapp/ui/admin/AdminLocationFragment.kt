package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.rst.gadissalonmanagementsystemapp.BuildConfig
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.SalonLocation
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentLocationBinding
import kotlinx.coroutines.launch

class AdminLocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private var isEditMode = false
    private var newLocationToSave: SalonLocation? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            // Get the key from the auto-generated BuildConfig class
            val apiKey = BuildConfig.MAPS_API_KEY
            Places.initialize(requireContext(), apiKey)
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        setupAutocompleteFragment()

        binding.editSaveButton.visibility = View.VISIBLE
        binding.editSaveButton.setOnClickListener {
            toggleEditMode()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadLocationData()
    }

    private fun setupAutocompleteFragment() {
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("AdminLocationFragment", "Place: ${place.name}, ${place.id}, ${place.latLng}")

                // A new location was selected from the search
                val latLng = place.latLng ?: return

                newLocationToSave = SalonLocation(
                    addressLine1 = place.name ?: "Unknown Location",
                    addressLine2 = place.address ?: "",
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                )

                // Update the UI immediately to show the new location
                updateUI(newLocationToSave!!)
            }

            override fun onError(status: Status) {
                Log.e("AdminLocationFragment", "An error occurred: $status")
            }
        })
    }

    private fun loadLocationData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getSalonLocation()
            if (result.isSuccess) {
                val location = result.getOrNull() ?: SalonLocation()
                updateUI(location)
            }
        }
    }

    private fun updateUI(location: SalonLocation) {
        // Update text fields and map marker
        binding.addressText.text = "${location.addressLine1}\n${location.addressLine2}"
        binding.address1Input.setText(location.addressLine1)
        binding.address2Input.setText(location.addressLine2)

        val latLng = LatLng(location.latitude, location.longitude)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng).title("Gadis Salon"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        googleMap.setOnCameraIdleListener {
            // This code will run once the map has finished moving
            if (_binding != null) {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
            }
            // Set the listener to null so it doesn't run again on future map movements
            googleMap.setOnCameraIdleListener(null)
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        if (isEditMode) {
            // Enter edit mode
            binding.addressText.visibility = View.GONE
            binding.editAddressLayout.visibility = View.VISIBLE
            binding.editSaveButton.text = "Save"
        } else {
            // Save the changes and exit edit mode
            saveLocationData()
        }
    }

    private fun saveLocationData() {
        val newAddress1 = binding.address1Input.text.toString()
        val newAddress2 = binding.address2Input.text.toString()
        // In a real app, you'd get new lat/lng from a map picker or geocoding

        val newLocation = SalonLocation(
            addressLine1 = newAddress1,
            addressLine2 = newAddress2
            // Keep old lat/lng for now
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.updateSalonLocation(newLocation)
            if (result.isSuccess) {
                Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                updateUI(newLocation) // Refresh the UI with the new data
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            // Exit edit mode regardless of success/failure
            binding.addressText.visibility = View.VISIBLE
            binding.editAddressLayout.visibility = View.GONE
            binding.editSaveButton.text = "Edit"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}