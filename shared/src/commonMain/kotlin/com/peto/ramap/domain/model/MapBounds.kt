package com.peto.ramap.domain.model

import kotlin.math.abs

/**
 * 지도 화면에 표시되는 위경도 경계 영역.
 *
 * [minLat], [maxLat], [minLng], [maxLng]로 사각형 형태의 viewport를 표현하고,
 * 지도 이동이나 확대/축소가 실제 조회와 클러스터 갱신에 의미 있는 변화인지 판단한다.
 */
data class MapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) {
    /** 현재 지도 영역의 중심 위도. */
    val centerLat: Double
        get() = (minLat + maxLat) / 2

    /** 현재 지도 영역의 중심 경도. */
    val centerLng: Double
        get() = (minLng + maxLng) / 2

    /** 현재 지도 영역이 세로 방향으로 포함하는 위도 범위. */
    val latSpan: Double
        get() = maxLat - minLat

    /** 현재 지도 영역이 가로 방향으로 포함하는 경도 범위. */
    val lngSpan: Double
        get() = maxLng - minLng

    /** [location]이 현재 지도 영역 안에 포함되는지 반환한다. */
    fun contains(location: Location): Boolean =
        location.lat in minLat..maxLat &&
            location.lng in minLng..maxLng

    /**
     * 이전 지도 영역과 비교해 화면 영역이 충분히 달라졌는지 판단한다.
     *
     * 카메라 이동 종료 이벤트는 작은 드래그나 SDK 좌표 오차에도 자주 발생할 수 있으므로,
     * 중심점 이동이 이전 화면 span의 20% 이상이거나 확대/축소 span 변화가 15% 이상일 때만
     * 의미 있는 변경으로 본다.
     *
     * [centerShiftRatio]는 이전 영역의 span 대비 중심 좌표가 얼마나 이동해야 하는지,
     * [zoomShiftRatio]는 이전 영역의 span 대비 위경도 범위가 얼마나 달라져야 하는지를 나타낸다.
     */
    fun hasMeaningfulViewportChangeFrom(
        other: MapBounds,
        centerShiftRatio: Double = DEFAULT_CENTER_SHIFT_RATIO,
        zoomShiftRatio: Double = DEFAULT_ZOOM_SHIFT_RATIO,
    ): Boolean {
        val isCenterShifted =
            abs(centerLat - other.centerLat) >= other.latSpan * centerShiftRatio ||
                abs(centerLng - other.centerLng) >= other.lngSpan * centerShiftRatio
        val isZoomShifted = hasMeaningfulZoomChangeFrom(other, zoomShiftRatio)

        return isCenterShifted || isZoomShifted
    }

    /**
     * 이전 지도 영역과 비교해 확대/축소 비율만 충분히 달라졌는지 판단한다.
     *
     * 중심 좌표 이동은 무시하고 화면 위경도 span 변화만 비교한다.
     * [zoomShiftRatio]는 이전 영역의 span 대비 현재 span이 얼마나 달라져야 하는지를 나타낸다.
     */
    fun hasMeaningfulZoomChangeFrom(
        other: MapBounds,
        zoomShiftRatio: Double = DEFAULT_ZOOM_SHIFT_RATIO,
    ): Boolean =
        hasMeaningfulRatioChange(
            current = latSpan,
            previous = other.latSpan,
            ratio = zoomShiftRatio,
        ) ||
            hasMeaningfulRatioChange(
                current = lngSpan,
                previous = other.lngSpan,
                ratio = zoomShiftRatio,
            )

    private fun hasMeaningfulRatioChange(
        current: Double,
        previous: Double,
        ratio: Double,
    ): Boolean {
        if (previous == 0.0) return current != 0.0
        return abs(current - previous) / abs(previous) >= ratio
    }

    companion object {
        private const val DEFAULT_CENTER_SHIFT_RATIO = 0.2
        private const val DEFAULT_ZOOM_SHIFT_RATIO = 0.15
    }
}
