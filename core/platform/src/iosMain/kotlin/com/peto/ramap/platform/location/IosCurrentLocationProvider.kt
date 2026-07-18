package com.peto.ramap.platform.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosCurrentLocationProvider : CurrentLocationProvider {
    private val locationManager = CLLocationManager()
    private var continuation: CancellableContinuation<PlatformLocation?>? = null
    private val delegate = IosCurrentLocationDelegate(::handleLocation)

    init {
        locationManager.delegate = delegate
    }

    override suspend fun fetchCurrentLocation(): PlatformLocation? =
        suspendCancellableCoroutine { newContinuation ->
            continuation?.cancel()
            continuation = newContinuation
            newContinuation.invokeOnCancellation {
                locationManager.stopUpdatingLocation()
                if (continuation === newContinuation) continuation = null
            }
            locationManager.requestLocation()
        }

    private fun handleLocation(location: CLLocation?) {
        val activeContinuation = continuation ?: return
        continuation = null
        if (!activeContinuation.isActive) return

        activeContinuation.resume(
            location?.coordinate?.useContents {
                PlatformLocation(
                    latitude = latitude,
                    longitude = longitude,
                )
            },
        )
    }
}
