package com.peto.ramap.core.config

import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds

/**
 * 앱이 지도 최초 진입 시 사용할 기본 카메라 값
 */
object DefaultMapConfig {
    const val LATITUDE = 37.402005
    const val LONGITUDE = 127.108621
    const val ZOOM_LEVEL = 15

    val location: Location =
        Location(
            lat = LATITUDE,
            lng = LONGITUDE,
        )

    val bounds: MapBounds =
        MapBounds(
            minLat = LATITUDE - DEFAULT_LATITUDE_SPAN / 2,
            maxLat = LATITUDE + DEFAULT_LATITUDE_SPAN / 2,
            minLng = LONGITUDE - DEFAULT_LONGITUDE_SPAN / 2,
            maxLng = LONGITUDE + DEFAULT_LONGITUDE_SPAN / 2,
        )

    private const val DEFAULT_LATITUDE_SPAN = 0.02
    private const val DEFAULT_LONGITUDE_SPAN = 0.02
}
