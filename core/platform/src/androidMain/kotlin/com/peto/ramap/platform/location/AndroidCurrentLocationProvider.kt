package com.peto.ramap.platform.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidCurrentLocationProvider(
    context: Context,
) : CurrentLocationProvider {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(): PlatformLocation? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }

            locationClient
                .getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token,
                ).addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(
                            location?.let {
                                PlatformLocation(
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                )
                            },
                        )
                    }
                }.addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }.addOnCanceledListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
}
