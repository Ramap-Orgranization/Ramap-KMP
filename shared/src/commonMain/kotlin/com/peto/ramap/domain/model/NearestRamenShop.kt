package com.peto.ramap.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun List<RamenShop>.nearestTo(location: Location?): RamenShop? {
    if (location == null) return null

    return minByOrNull { shop ->
        haversineDistanceMeters(
            from = location,
            to = shop.location,
        )
    }
}

private fun haversineDistanceMeters(
    from: Location,
    to: Location,
): Double {
    val latDistance = (to.lat - from.lat).toRadians()
    val lngDistance = (to.lng - from.lng).toRadians()
    val fromLat = from.lat.toRadians()
    val toLat = to.lat.toRadians()
    val a =
        sin(latDistance / 2).pow(2) +
            cos(fromLat) * cos(toLat) * sin(lngDistance / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_METERS * c
}

private fun Double.toRadians(): Double = this / 180.0 * kotlin.math.PI

private const val EARTH_RADIUS_METERS = 6_371_000.0
