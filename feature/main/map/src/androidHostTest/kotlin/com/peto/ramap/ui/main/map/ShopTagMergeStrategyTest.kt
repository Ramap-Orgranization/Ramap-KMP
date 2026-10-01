package com.peto.ramap.ui.main.map

import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopTagMergeStrategyTest {
    @Test
    fun `리프와 중첩 클러스터 태그의 매장을 모두 보존한다`() {
        val first = ramenShop("first")
        val second = ramenShop("second")
        val third = ramenShop("third")

        assertEquals(listOf(first, second, third), mergeShopTags(listOf(first, listOf(second, third))))
    }

    private fun ramenShop(id: String) =
        RamenShop(
            id = id,
            name = id,
            address = "서울",
            location = Location(37.551, 126.921),
            kakaoPlaceUrl = null,
            instagramUrl = null,
            menuCategories = MenuCategories(listOf(Category.SHOYU)),
            isVisible = true,
            createdAt = "",
            updatedAt = "",
        )
}
