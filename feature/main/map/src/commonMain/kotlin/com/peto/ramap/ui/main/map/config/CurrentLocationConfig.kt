package com.peto.ramap.ui.main.map.config

internal object CurrentLocationConfig {
    const val REQUEST_TIMEOUT_MILLIS = 10_000L
    const val ZOOM_LEVEL = 15.toDouble()

    fun zoomForCurrentLocation(currentZoom: Double): Double =
        maxOf(
            currentZoom,
            ZOOM_LEVEL,
        )
}
