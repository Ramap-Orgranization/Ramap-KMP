package com.peto.ramap.ui.main.map.config

import kotlin.test.Test
import kotlin.test.assertEquals

class MapInteractionConfigTest {
    @Test
    fun `숨김 매장 마커는 반투명으로 렌더링한다`() {
        assertEquals(0.5f, MapInteractionConfig.HIDDEN_SHOP_ALPHA)
    }
}
