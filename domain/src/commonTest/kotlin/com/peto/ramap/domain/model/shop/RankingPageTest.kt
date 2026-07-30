package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.ShopRankings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RankingPageTest {
    @Test
    fun `다음 커서가 있으면 다음 페이지가 존재한다`() {
        val cursor = RankingCursor(likeCount = 3, name = "매장", shopId = "shop-id")

        assertTrue(RankingPage(ShopRankings(emptyList()), cursor).hasNext)
        assertFalse(RankingPage(ShopRankings(emptyList()), null).hasNext)
    }
}
