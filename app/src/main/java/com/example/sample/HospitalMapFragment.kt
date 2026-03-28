package com.example.sample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest

class HospitalMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var btnCallEmergency: MaterialButton
    private lateinit var tvNearestHospital: TextView

    private var emergencyPhoneNumber: String? = null
    private val defaultLocation = LatLng(15.4828, 120.5943) // Tarlac City Default

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableUserLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission needed to find hospitals.", Toast.LENGTH_LONG).show()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hospital_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        btnCallEmergency = view.findViewById(R.id.btnCallEmergency)
        tvNearestHospital = view.findViewById(R.id.tvNearestHospital)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Google Places API
        val appInfo = requireContext().packageManager.getApplicationInfo(requireContext().packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        if (apiKey != null && !Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }
        placesClient = Places.createClient(requireContext())

        // Initialize
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup Dialer
        btnCallEmergency.setOnClickListener {
            emergencyPhoneNumber?.let { number ->
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$number")
                startActivity(dialIntent)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))

                // SAFELY LAUNCH GOOGLE MAPS
                tvNearestHospital.text = "Hospitals found in your area."
                btnCallEmergency.isEnabled = true

                btnCallEmergency.setOnClickListener {
                    val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=hospitals")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    try {
                        startActivity(mapIntent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Google Maps app is not installed.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Searching for GPS signal...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchNearestHospitalFromPlacesAPI(userLocation: LatLng) {

        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.PHONE_NUMBER, Place.Field.TYPES)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        placesClient.findCurrentPlace(request)
            .addOnSuccessListener { response ->
                val hospitals = response.placeLikelihoods
                    .filter { it.place.types?.contains(Place.Type.HOSPITAL) == true }
                    .sortedBy { it.likelihood } // Likelihood often correlates with proximity here
                    .reversed()

                if (hospitals.isNotEmpty()) {
                    for (likelihood in hospitals) {
                        val place = likelihood.place
                        place.latLng?.let { latLng ->
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(place.name)
                                    .snippet("Phone: ${place.phoneNumber ?: "N/A"}")
                            )
                        }
                    }

                    val nearestHospital = hospitals[0].place
                    tvNearestHospital.text = "Nearest: ${nearestHospital.name}"

                    if (!nearestHospital.phoneNumber.isNullOrEmpty()) {
                        emergencyPhoneNumber = nearestHospital.phoneNumber
                        btnCallEmergency.isEnabled = true
                    } else {
                        tvNearestHospital.text = "${nearestHospital.name} (No phone listed)"
                        btnCallEmergency.isEnabled = false
                    }
                } else {
                    tvNearestHospital.text = "No hospitals found nearby."
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error finding hospitals: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}