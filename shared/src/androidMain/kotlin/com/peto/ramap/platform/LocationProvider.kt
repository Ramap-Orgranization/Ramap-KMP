package com.peto.ramap.platform

import android.content.Context
import android.location.LocationManager
import com.peto.ramap.domain.model.Location
import android.location.Location as AndroidLocation

/**
 * Android 시스템 위치 서비스에서 마지막으로 알려진 위치를 조회한다.
 *
 * 이 클래스는 권한을 요청하거나 확인하지 않는다. 호출자는 `LocationPermissionGenerator` 등을 통해
 * 위치 권한이 허용된 뒤 [position]을 호출해야 한다.
 */
internal actual class LocationProvider(
    private val context: Context,
) {
    /**
     * GPS와 네트워크 provider의 last known location 중 가장 최신 위치를 반환한다.
     */
    actual suspend fun position(): Location? {
        val locationManager = locationManager() ?: return null
        val latestLocation = findLatestKnownLocation(locationManager) ?: return null
        return latestLocation.toDomainLocation()
    }

    private fun locationManager(): LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

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
