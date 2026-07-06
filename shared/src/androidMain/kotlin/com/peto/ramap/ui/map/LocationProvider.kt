package com.peto.ramap.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.kakao.vectormap.KakaoMap

/**
 * 지도에서 사용하는 마지막 위치 조회를 캡슐화한다.
 */
internal class LocationProvider(
    private val context: Context,
) {
    fun moveToLastKnownLocation(
        kakaoMap: KakaoMap,
        cameraController: KakaoCameraController,
    ): Location? {
        val location = currentLocation() ?: return null
        cameraController.moveToLocation(kakaoMap, location)
        return location
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun currentLocation(): Location? {
        if (!hasLocationPermission()) return null

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        return LOCATION_PROVIDERS
            .mapNotNull { provider ->
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }.maxByOrNull { it.time }
    }

    private companion object {
        private val LOCATION_PROVIDERS =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
            )
    }
}
