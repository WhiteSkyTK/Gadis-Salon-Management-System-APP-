package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rst.gadissalonmanagementsystemapp.FirebaseManager
import com.rst.gadissalonmanagementsystemapp.R
import com.rst.gadissalonmanagementsystemapp.SalonLocation
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentLocationBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch

class LocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        // Check for internet before initializing the map
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            binding.shimmerViewContainer.startShimmer()
            binding.shimmerViewContainer.visibility = View.VISIBLE
            binding.contentContainer.visibility = View.GONE

            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } else {
            showOfflineUI(true)
        }
    }

    // This function is called when the map is fully loaded
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Now that the map is ready, fetch the location data from Firebase
        loadLocationFromFirebase()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun loadLocationFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseManager.getSalonLocation()
            if (view == null) return@launch

            // --- STOP SHIMMER & SHOW CONTENT ---
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE

            if (result.isSuccess) {
                val location = result.getOrNull() ?: SalonLocation()
                updateMapAndUI(location)
            } else {
                Log.e("LocationFragment", "Failed to fetch salon location", result.exceptionOrNull())
                updateMapAndUI(SalonLocation())
            }
        }
    }

    private fun updateMapAndUI(location: SalonLocation) {
        // Update the address text in the bottom card
        binding.addressText.text = "${location.addressLine1}\n${location.addressLine2}"

        // Create a LatLng object from the fetched coordinates
        val salonLatLng = LatLng(location.latitude, location.longitude)

        // Add a marker (pin) at the location
        googleMap.addMarker(MarkerOptions().position(salonLatLng).title("Gadis Salon"))

        // Move the camera to the location and zoom in
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(salonLatLng, 16f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}