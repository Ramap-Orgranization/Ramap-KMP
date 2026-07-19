package com.peto.ramap.ui.main.map.config

internal object CurrentLocationConfig {
    const val REQUEST_TIMEOUT_MILLIS = 10_000L

    fun zoomForCurrentLocation(currentZoom: Double): Double =
        maxOf(
            currentZoom,
            DefaultMapConfig.ZOOM_LEVEL.toDouble(),
        )
}
