package com.peto.ramap.data.datasource.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopTextSearchFilterTest {
    @Test
    fun `텍스트 검색은 운영상 노출 매장만 대상으로 한다`() {
        val filter = ShopTextSearchFilter.forVisibleShops("%라멘%")

        assertTrue(filter.isVisible)
        assertEquals("%라멘%", filter.pattern)
        assertEquals(
            listOf("name", "address", "phone", "business_hours", "business_hours_notice"),
            filter.columns,
        )
    }
}
