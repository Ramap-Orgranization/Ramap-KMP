package com.peto.ramap.domain.model.rank

import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class RankedShopsTest {
    @Test
    fun `첫 페이지에 순위를 부여한다`() {
        val rankings =
            listOf(
                ranking(id = "first", likeCount = 5),
                ranking(id = "second", likeCount = 5),
                ranking(id = "third", likeCount = 3),
            )

        val rankedShops = RankedShops(ShopRankings(rankings))

        assertEquals(listOf(1, 1, 2), rankedShops.map(RankedShop::rank))
    }

    @Test
    fun `다음 페이지의 중복을 제외하고 이전 페이지의 dense rank를 잇는다`() {
        val firstPage =
            RankedShops(
                ShopRankings(
                    listOf(
                        ranking(id = "first", likeCount = 5),
                        ranking(id = "second", likeCount = 3),
                    ),
                ),
            )

        val result =
            firstPage.appendNextPage(
                ShopRankings(
                    listOf(
                        ranking(id = "second", likeCount = 3),
                        ranking(id = "third", likeCount = 3),
                        ranking(id = "fourth", likeCount = 1),
                    ),
                ),
            )

        assertEquals(listOf("first", "second", "third", "fourth"), result.map { it.ranking.shop.id })
        assertEquals(listOf(1, 2, 2, 3), result.map(RankedShop::rank))
    }

    @Test
    fun `다음 페이지 안에서 반복된 매장은 첫 항목만 유지한다`() {
        val firstPage =
            RankedShops(
                ShopRankings(listOf(ranking(id = "first", likeCount = 5))),
            )

        val result =
            firstPage.appendNextPage(
                ShopRankings(
                    listOf(
                        ranking(id = "second", likeCount = 3),
                        ranking(id = "second", likeCount = 2),
                        ranking(id = "third", likeCount = 1),
                    ),
                ),
            )

        assertEquals(listOf("first", "second", "third"), result.map { it.ranking.shop.id })
        assertEquals(listOf(5L, 3L, 1L), result.map { it.ranking.likeCount })
        assertEquals(listOf(1, 2, 3), result.map(RankedShop::rank))
    }

    private fun ranking(
        id: String,
        likeCount: Long,
    ): ShopRanking = ShopRanking(shop = ramenShopFixture(id = id), likeCount = likeCount)
}
