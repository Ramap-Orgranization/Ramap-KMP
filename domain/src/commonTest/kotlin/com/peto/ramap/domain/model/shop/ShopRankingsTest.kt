package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopRankingsTest {
    @Test
    fun `카테고리는 OR로 적용하고 숨긴 매장과 다른 지역을 제외한다`() {
        val rankings =
            ShopRankings(
                listOf(
                    ranking("a", "가게", "서울특별시 마포구", 5, listOf(Category.SHOYU)),
                    ranking("b", "나게", "서울 마포구", 4, listOf(Category.MISO)),
                    ranking("c", "다게", "부산광역시 중구", 3, listOf(Category.MISO)),
                    ranking("d", "라게", "서울특별시 종로구", 2, listOf(Category.MISO)),
                ),
            )

        val result =
            rankings.filterAndRank(
                areaFilter = AreaFilter.Selected(AdministrativeArea.SEOUL),
                selectedCategories = setOf(Category.SHOYU, Category.MISO),
                hiddenShopIds = setOf("d"),
            )

        assertEquals(listOf("a", "b"), result.map { rankedShop -> rankedShop.ranking.shop.id })
    }

    @Test
    fun `좋아요 내림차순 이름 ID 순으로 정렬하고 공동 dense 순위를 매긴다`() {
        val rankings =
            ShopRankings(
                listOf(
                    ranking("c", "나", "서울", 2),
                    ranking("b", "가", "서울", 5),
                    ranking("a", "가", "서울", 5),
                    ranking("d", "다", "서울", 1),
                ),
            )

        val result = rankings.filterAndRank(AreaFilter.Nationwide, emptySet(), emptySet())

        assertEquals(listOf("a", "b", "c", "d"), result.map { rankedShop -> rankedShop.ranking.shop.id })
        assertEquals(listOf(1, 1, 2, 3), result.map(RankedShop::rank))
    }

    private fun ranking(
        id: String,
        name: String,
        address: String,
        likeCount: Long,
        categories: List<Category> = listOf(Category.SHOYU),
    ): ShopRanking =
        ShopRanking(
            shop =
                RamenShop(
                    id = id,
                    kakaoPlaceId = null,
                    name = name,
                    address = address,
                    location = Location(0.0, 0.0),
                    kakaoPlaceUrl = null,
                    phone = null,
                    businessHours = null,
                    instagramUrl = null,
                    kakaoRating = null,
                    menuCategories = MenuCategories(categories),
                    isVisible = true,
                    createdAt = "",
                    updatedAt = "",
                ),
            likeCount = likeCount,
        )
}
