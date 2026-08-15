package com.peto.ramap.ui.main.map

import com.peto.ramap.ui.main.map.config.CurrentLocationConfig
import com.peto.ramap.ui.main.map.config.DefaultMapConfig
import com.peto.ramap.ui.main.map.model.location.CurrentLocationRequestState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrentLocationPolicyTest {
    @Test
    fun `기본 줌보다 축소된 경우 현재 위치 줌은 기본 줌을 사용한다`() {
        val zoom = CurrentLocationConfig.zoomForCurrentLocation(currentZoom = 10.0)

        assertEquals(DefaultMapConfig.ZOOM_LEVEL.toDouble(), zoom)
    }

    @Test
    fun `기본 줌 이상인 경우 현재 위치 줌은 기존 줌을 유지한다`() {
        val currentZoom = 17.5

        val zoom = CurrentLocationConfig.zoomForCurrentLocation(currentZoom)

        assertEquals(currentZoom, zoom)
    }

    @Test
    fun `위치 요청은 시작 후 완료하면 대기 상태로 돌아간다`() {
        val loading = CurrentLocationRequestState.Idle.start()
        val completed = loading.finish()

        assertTrue(loading.isLoading)
        assertFalse(completed.isLoading)
    }

    @Test
    fun `조회 중 다시 시작해도 조회 상태는 하나만 유지한다`() {
        val loading = CurrentLocationRequestState.Idle.start()

        assertEquals(CurrentLocationRequestState.Loading, loading.start())
    }

    @Test
    fun `위치 요청은 타임아웃 후 대기 상태로 돌아간다`() {
        val loading = CurrentLocationRequestState.Idle.start()
        val timedOut = loading.timeout()

        assertEquals(10_000L, CurrentLocationConfig.REQUEST_TIMEOUT_MILLIS)
        assertEquals(CurrentLocationRequestState.Idle, timedOut)
    }
}
