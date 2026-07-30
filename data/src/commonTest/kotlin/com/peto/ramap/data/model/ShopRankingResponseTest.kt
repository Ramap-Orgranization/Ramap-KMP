package com.peto.ramap.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopRankingResponseTest {
    @Test
    fun `랭킹 응답의 매장과 좋아요 수를 도메인으로 변환한다`() {
        val response =
            ShopRankingResponse(
                id = "shop",
                name = "매장",
                address = "서울특별시 마포구",
                lat = 37.0,
                lng = 127.0,
                menuCategoryIds = listOf("shoyu"),
                isVisible = true,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                likeCount = 7,
            )

        val ranking = response.toDomain()

        assertEquals("shop", ranking.shop.id)
        assertEquals(7, ranking.likeCount)
    }
}
