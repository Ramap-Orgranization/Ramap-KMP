package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.data.model.ShopRankingResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopRankingResponsesTest {
    @Test
    fun `limit 초과 응답을 제외하고 포함한 마지막 매장으로 다음 커서를 만든다`() {
        val page = listOf(response("first", 3), response("second", 2)).toDomain(limit = 1)

        assertEquals(listOf("first"), page.items.map { item -> item.shop.id })
        assertEquals(3, page.nextCursor?.likeCount)
        assertEquals("매장-first", page.nextCursor?.name)
        assertEquals("first", page.nextCursor?.shopId)
    }

    private fun response(
        id: String,
        likeCount: Long,
    ): ShopRankingResponse =
        ShopRankingResponse(
            id = id,
            name = "매장-$id",
            address = "서울 마포구",
            lat = 37.0,
            lng = 127.0,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            likeCount = likeCount,
        )
}
