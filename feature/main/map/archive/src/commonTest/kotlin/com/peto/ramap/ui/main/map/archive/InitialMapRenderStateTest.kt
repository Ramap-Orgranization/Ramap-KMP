package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.ui.main.map.archive.model.InitialMapRenderState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitialMapRenderStateTest {
    @Test
    fun `초기 카메라를 적용하기 전 기본 위치의 렌더링은 준비 완료로 알리지 않는다`() {
        val state = InitialMapRenderState()

        assertFalse(state.onFrameRendered(fully = true, stable = true))
        state.cameraConfigured()
        assertTrue(state.onFrameRendered(fully = true, stable = true))
    }

    @Test
    fun `타일이 덜 그려졌거나 화면이 움직이면 준비 완료를 기다린다`() {
        val state = InitialMapRenderState()
        state.cameraConfigured()

        assertFalse(state.onFrameRendered(fully = false, stable = true))
        assertFalse(state.onFrameRendered(fully = true, stable = false))
        assertTrue(state.onFrameRendered(fully = true, stable = true))
    }

    @Test
    fun `사진이나 카메라가 다시 갱신되어도 최초 준비 완료는 한 번만 알린다`() {
        val state = InitialMapRenderState()
        state.cameraConfigured()

        assertTrue(state.onFrameRendered(fully = true, stable = true))
        state.cameraConfigured()
        assertFalse(state.onFrameRendered(fully = true, stable = true))
    }
}
