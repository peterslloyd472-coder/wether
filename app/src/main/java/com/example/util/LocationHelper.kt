package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val country: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, isFossEdition: Boolean = false): DeviceLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        var location: Location? = null

        // In FOSS Edition, strictly use Android System LocationManager (Zero Google Play Services)
        if (!isFossEdition) {
            // 1. Try Google Play Services FusedLocationProviderClient for Google Edition
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            location = suspendCancellableCoroutine { continuation ->
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }
        } catch (_: Exception) {
            // Ignore and fall back to LocationManager
        }
        }

        // 2. Fallback to Android System LocationManager if FusedClient was null or in FOSS mode
        if (location == null) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    if (locationManager?.isProviderEnabled(provider) == true) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            location = loc
                            break
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore
            }
        }

        if (location == null) {
            return@withContext null
        }

        // 3. Resolve City & Country Name via Geocoder
        val (cityName, country) = resolveCityAndCountry(context, location.latitude, location.longitude)

        DeviceLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = cityName,
            country = country
        )
    }

    suspend fun resolveCityAndCountry(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val addresses = suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            cont.resume(list)
                        }
                    }
                    val address = addresses.firstOrNull()
                    if (address != null) {
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "My Location"
                        val country = address.countryName ?: ""
                        return@withContext Pair(city, country)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()
                    if (address != null) {
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "My Location"
                        val country = address.countryName ?: ""
                        return@withContext Pair(city, country)
                    }
                }
            }
        } catch (_: Exception) {
            // Geocoder service could be temporarily unavailable
        }

        Pair("Current Location", "")
    }
}
