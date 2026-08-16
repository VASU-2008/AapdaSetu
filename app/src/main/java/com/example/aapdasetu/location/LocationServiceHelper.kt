package com.example.aapdasetu.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationData(
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val accuracyMeters: Float = 12.5f,
    val locationName: String = "Connaught Place, New Delhi",
    val isRealGps: Boolean = false
)

class LocationServiceHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationData())
    val locationState: StateFlow<LocationData> = _locationState.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        if (!hasLocationPermission()) {
            _locationState.value = LocationData(
                latitude = 28.6139,
                longitude = 77.2090,
                locationName = "New Delhi (Simulated GPS)",
                isRealGps = false
            )
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        _locationState.value = LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            locationName = String.format("%.4f° N, %.4f° E", location.latitude, location.longitude),
                            isRealGps = true
                        )
                    }
                }
                .addOnFailureListener {
                    // Fallback gracefully
                }
        } catch (_: Exception) {
            // Ignored
        }
    }
}
