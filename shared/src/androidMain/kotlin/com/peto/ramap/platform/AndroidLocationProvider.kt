package com.peto.ramap.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location as AndroidLocation
import android.location.LocationManager
import com.peto.ramap.domain.model.Location

internal class AndroidLocationProvider(
    private val context: Context,
) {
    suspend fun position(): Location? {
        val locationManager = locationManager() ?: return null
        val latestLocation = findLatestKnownLocation(locationManager) ?: return null
        return latestLocation.toDomainLocation()
    }

    private fun locationManager(): LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission")
    private fun findLatestKnownLocation(locationManager: LocationManager): AndroidLocation? {
        var latestLocation: AndroidLocation? = null

        for (provider in LOCATION_PROVIDERS) {
            val location = lastKnownLocationOrNull(locationManager, provider) ?: continue
            if (latestLocation == null || location.time > latestLocation.time) {
                latestLocation = location
            }
        }

        return latestLocation
    }

    private fun lastKnownLocationOrNull(
        locationManager: LocationManager,
        provider: String,
    ): AndroidLocation? {
        if (!locationManager.isProviderEnabled(provider)) return null
        return locationManager.getLastKnownLocation(provider)
    }

    private fun AndroidLocation.toDomainLocation(): Location =
        Location(
            lat = latitude,
            lng = longitude,
        )

    private companion object {
        private val LOCATION_PROVIDERS =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
            )
    }
}
