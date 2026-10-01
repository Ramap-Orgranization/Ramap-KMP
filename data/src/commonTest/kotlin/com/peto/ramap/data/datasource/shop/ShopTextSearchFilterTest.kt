package com.peto.ramap.data.datasource.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopTextSearchFilterTest {
    @Test
    fun `텍스트 검색은 매장 텍스트 컬럼을 대상으로 한다`() {
        val filter = ShopTextSearchFilter("%라멘%")

        assertEquals("%라멘%", filter.pattern)
        assertEquals(
            listOf("name", "address", "business_hours_notice", "kakao_place_url", "naver_place_url"),
            filter.columns,
        )
    }
}
