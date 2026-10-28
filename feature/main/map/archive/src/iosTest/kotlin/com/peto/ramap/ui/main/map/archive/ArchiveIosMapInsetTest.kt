package com.peto.ramap.ui.main.map.archive

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveIosMapInsetTest {
    @Test
    fun `선택 매장 여백은 화면 배율과 관계없이 포인트 기준으로 계산한다`() {
        assertEquals(280.0, selectedMarkerInsetPoints(2400, 3f, hasSelection = true, shopCount = 1), absoluteTolerance = 0.001)
        assertEquals(280.0, selectedMarkerInsetPoints(1600, 2f, hasSelection = true, shopCount = 1), absoluteTolerance = 0.001)
        assertEquals(0.0, selectedMarkerInsetPoints(2400, 3f, hasSelection = false, shopCount = 1), absoluteTolerance = 0.001)
        assertEquals(0.0, selectedMarkerInsetPoints(2400, 3f, hasSelection = true, shopCount = 2), absoluteTolerance = 0.001)
    }
}
