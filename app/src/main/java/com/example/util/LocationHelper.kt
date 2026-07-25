package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.example.data.api.ApiClient
import com.example.data.model.CityEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentUserLocation(): CityEntity? = withContext(Dispatchers.IO) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } catch (e: Exception) {
                fusedLocationClient.lastLocation.await()
            }

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val cityName = reverseGeocodeCity(lat, lon)
                return@withContext CityEntity(
                    id = "current_location_${lat}_${lon}",
                    name = cityName,
                    country = "Current Location",
                    latitude = lat,
                    longitude = lon,
                    isDefault = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: If device location unavailable, return San Francisco / detected IP location
        return@withContext CityEntity(
            id = "auto_sf",
            name = "San Francisco",
            country = "United States",
            latitude = 37.7749,
            longitude = -122.4194,
            isDefault = true
        )
    }

    private fun reverseGeocodeCity(lat: Double, lon: Double): String {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: "My Location"
                } else {
                    "My Location"
                }
            } else {
                "My Location"
            }
        } catch (e: Exception) {
            "My Location"
        }
    }
}
