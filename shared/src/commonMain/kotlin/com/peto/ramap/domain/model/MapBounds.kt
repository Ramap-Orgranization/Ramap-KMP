package com.peto.ramap.domain.model

import kotlin.math.abs

data class MapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) {
    val centerLat: Double
        get() = (minLat + maxLat) / 2

    val centerLng: Double
        get() = (minLng + maxLng) / 2

    val latSpan: Double
        get() = maxLat - minLat

    val lngSpan: Double
        get() = maxLng - minLng

    fun contains(location: Location): Boolean =
        location.lat in minLat..maxLat &&
            location.lng in minLng..maxLng

    /**
     * 이전에 조회한 지도 영역과 비교해 새 API 호출이 필요한 수준의 변화인지 판단한다.
     *
     * 카메라 이동 종료 이벤트는 작은 드래그나 SDK 좌표 오차에도 자주 발생할 수 있으므로,
     * 중심점 이동이 이전 화면 span의 20% 이상이거나 확대/축소 span 변화가 15% 이상일 때만
     * 의미 있는 변경으로 본다.
     */
    fun isMeaningfullyDifferentFrom(
        other: MapBounds,
        centerShiftRatio: Double = DEFAULT_CENTER_SHIFT_RATIO,
        zoomShiftRatio: Double = DEFAULT_ZOOM_SHIFT_RATIO,
    ): Boolean {
        val isCenterShifted =
            abs(centerLat - other.centerLat) >= other.latSpan * centerShiftRatio ||
                abs(centerLng - other.centerLng) >= other.lngSpan * centerShiftRatio
        val isZoomShifted =
            latSpan.isMeaningfullyDifferentFrom(other.latSpan, zoomShiftRatio) ||
                lngSpan.isMeaningfullyDifferentFrom(other.lngSpan, zoomShiftRatio)

        return isCenterShifted || isZoomShifted
    }

    private fun Double.isMeaningfullyDifferentFrom(
        other: Double,
        ratio: Double,
    ): Boolean {
        if (other == 0.0) return this != 0.0
        return abs(this - other) / abs(other) >= ratio
    }

    companion object {
        private const val DEFAULT_CENTER_SHIFT_RATIO = 0.2
        private const val DEFAULT_ZOOM_SHIFT_RATIO = 0.15
    }
}
