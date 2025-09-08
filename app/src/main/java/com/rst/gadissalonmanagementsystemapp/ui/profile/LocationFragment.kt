package com.rst.gadissalonmanagementsystemapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rst.gadissalonmanagementsystemapp.R

class LocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the map fragment and tell it we're ready for the callback
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // This function is called when the map is fully loaded and ready to use
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Coordinates for Rosebank College, 23 Jorissen St
        val salonLocation = LatLng(-26.1919, 28.0343)

        // Add a marker (pin) at the location
        googleMap.addMarker(MarkerOptions().position(salonLocation).title("Gadis Salon (Rosebank College)"))

        // Move the camera to the location and zoom in
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(salonLocation, 15f))
    }
}