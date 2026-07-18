package com.peto.ramap.platform.location

fun interface CurrentLocationProvider {
    suspend fun fetchCurrentLocation(): PlatformLocation?
}
