package com.peto.ramap.ui.main.map.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapInteractionConfigTest {
    @Test
    fun `숨김 매장 마커는 반투명으로 렌더링한다`() {
        assertEquals(0.5f, MapInteractionConfig.HIDDEN_SHOP_ALPHA)
    }

    @Test
    fun `클러스터 최대 줌은 SDK 인덱스 범위 안에 있다`() {
        assertTrue(MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL < SDK_MAX_ZOOM_LEVEL)
    }

    private companion object {
        const val SDK_MAX_ZOOM_LEVEL = 21
    }
}
