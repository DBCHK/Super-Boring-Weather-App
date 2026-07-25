package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.data.model.CityEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class LocationResult {
    data class Success(val city: CityEntity) : LocationResult()
    /** App permission missing — request ACCESS_FINE/COARSE. */
    data object PermissionRequired : LocationResult()
    /** Device location services (GPS/network) are off — open system settings. */
    data object ServicesDisabled : LocationResult()
    data class Failed(val message: String) : LocationResult()
}

class LocationHelper(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentUserLocation(): CityEntity? {
        return when (val result = resolveLocation()) {
            is LocationResult.Success -> result.city
            else -> null
        }
    }

    /**
     * Full resolution path used by UI so it can prompt for services / permission.
     */
    @SuppressLint("MissingPermission")
    suspend fun resolveLocation(): LocationResult = withContext(Dispatchers.IO) {
        if (!isLocationServicesEnabled()) {
            return@withContext LocationResult.ServicesDisabled
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } catch (_: Exception) {
                try {
                    fusedLocationClient.lastLocation.await()
                } catch (_: Exception) {
                    null
                }
            }

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val cityName = reverseGeocodeCity(lat, lon)
                return@withContext LocationResult.Success(
                    CityEntity(
                        id = "current_location_${lat}_${lon}",
                        name = cityName,
                        country = "Current Location",
                        latitude = lat,
                        longitude = lon,
                        isDefault = true
                    )
                )
            }
            LocationResult.Failed("Could not determine location")
        } catch (e: SecurityException) {
            LocationResult.PermissionRequired
        } catch (e: Exception) {
            e.printStackTrace()
            LocationResult.Failed(e.message ?: "Location error")
        }
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
        } catch (_: Exception) {
            "My Location"
        }
    }
}
