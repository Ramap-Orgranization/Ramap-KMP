package com.peto.ramap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapBoundsTest {
    @Test
    fun `위치 목록으로 경계 영역을 만든다`() {
        val bounds =
            MapBounds.fromLocations(
                listOf(
                    Location(lat = 37.60, lng = 127.10),
                    Location(lat = 37.50, lng = 126.90),
                    Location(lat = 37.55, lng = 127.20),
                    Location(lat = 37.45, lng = 127.00),
                ),
            )

        assertEquals(
            MapBounds(
                minLat = 37.45,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.20,
            ),
            bounds,
        )
    }

    @Test
    fun `지도 영역의 중심과 span을 계산한다`() {
        val bounds =
            MapBounds(
                minLat = 37.50,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.10,
            )

        assertEquals(37.55, bounds.centerLat, DOUBLE_TOLERANCE)
        assertEquals(127.00, bounds.centerLng, DOUBLE_TOLERANCE)
        assertEquals(0.10, bounds.latSpan, DOUBLE_TOLERANCE)
        assertEquals(0.20, bounds.lngSpan, DOUBLE_TOLERANCE)
    }

    @Test
    fun `지도 영역을 각 방향으로 비율만큼 확장한다`() {
        val bounds =
            MapBounds(
                minLat = 10.0,
                maxLat = 20.0,
                minLng = 30.0,
                maxLng = 50.0,
            )

        assertEquals(
            MapBounds(
                minLat = 5.0,
                maxLat = 25.0,
                minLng = 20.0,
                maxLng = 60.0,
            ),
            bounds.expandBy(0.5),
        )
    }

    @Test
    fun `다른 지도 영역의 경계를 포함하면 완전 포함으로 판단한다`() {
        val bounds = MapBounds(minLat = 10.0, maxLat = 20.0, minLng = 30.0, maxLng = 50.0)

        assertTrue(bounds.contains(MapBounds(minLat = 10.0, maxLat = 18.0, minLng = 32.0, maxLng = 50.0)))
        assertFalse(bounds.contains(MapBounds(minLat = 9.9, maxLat = 18.0, minLng = 32.0, maxLng = 50.0)))
    }

    @Test
    fun `중심 이동이 기준보다 작으면 의미 있는 변경으로 판단하지 않는다`() {
        val previous =
            MapBounds(
                minLat = 37.50,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.00,
            )
        val current =
            previous.copy(
                minLat = previous.minLat + 0.01,
                maxLat = previous.maxLat + 0.01,
            )

        assertFalse(current.hasMeaningfulViewportChangeFrom(previous))
    }

    @Test
    fun `중심 이동이 기준 이상이면 의미 있는 변경으로 판단한다`() {
        val previous =
            MapBounds(
                minLat = 37.50,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.00,
            )
        val current =
            previous.copy(
                minLat = previous.minLat + 0.02,
                maxLat = previous.maxLat + 0.02,
            )

        assertTrue(current.hasMeaningfulViewportChangeFrom(previous))
    }

    @Test
    fun `span 변화가 기준 이상이면 의미 있는 변경으로 판단한다`() {
        val previous =
            MapBounds(
                minLat = 37.50,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.00,
            )
        val current =
            previous.copy(
                maxLat = previous.maxLat + 0.015,
            )

        assertTrue(current.hasMeaningfulViewportChangeFrom(previous))
    }

    @Test
    fun `줌 변화 판단은 중심 이동을 무시하고 span 변화만 비교한다`() {
        val previous =
            MapBounds(
                minLat = 37.50,
                maxLat = 37.60,
                minLng = 126.90,
                maxLng = 127.00,
            )
        val current =
            previous.copy(
                minLat = previous.minLat + 0.02,
                maxLat = previous.maxLat + 0.02,
            )

        assertFalse(current.hasMeaningfulZoomChangeFrom(previous))
    }

    companion object {
        private const val DOUBLE_TOLERANCE = 0.000001
    }
}
