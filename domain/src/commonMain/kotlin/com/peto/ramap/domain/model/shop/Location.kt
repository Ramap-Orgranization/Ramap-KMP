package com.peto.ramap.domain.model.shop

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Location(
    val lat: Double,
    val lng: Double,
) {
    /**
     * 현재 좌표에서 [other]까지의 지표면 최단 거리를 미터 단위로 반환한다.
     */
    fun distanceMetersTo(other: Location): Double {
        val latDistance = (other.lat - lat).toRadians()
        val lngDistance = (other.lng - lng).toRadians()
        val fromLat = lat.toRadians()
        val toLat = other.lat.toRadians()
        val haversine =
            sin(latDistance / 2).pow(2) +
                cos(fromLat) * cos(toLat) * sin(lngDistance / 2).pow(2)

        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private fun Double.toRadians(): Double = this / 180.0 * kotlin.math.PI

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
