package com.peto.ramap.ui.main.map

import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopClusterPopupPolicyTest {
    @Test
    fun `같은 좌표의 매장들은 팝업 대상이다`() {
        assertTrue(hasOnlyOverlappingMarkers(listOf(shop("first"), shop("second"))))
    }

    @Test
    fun `1미터 이내의 매장들은 팝업 대상이다`() {
        assertTrue(hasOnlyOverlappingMarkers(listOf(shop("first"), shop("second", latitudeOffset = 0.000005))))
    }

    @Test
    fun `떨어진 매장이 포함되면 팝업 대상이 아니다`() {
        assertFalse(hasOnlyOverlappingMarkers(listOf(shop("first"), shop("second", latitudeOffset = 0.00002))))
    }

    @Test
    fun `겹친 매장쌍에 떨어진 매장이 섞이면 팝업 대상이 아니다`() {
        assertFalse(
            hasOnlyOverlappingMarkers(
                listOf(
                    shop("first"),
                    shop("second", latitudeOffset = 0.000005),
                    shop("third", latitudeOffset = 0.00002),
                ),
            ),
        )
    }

    @Test
    fun `빈 목록과 단일 매장은 팝업 대상이 아니다`() {
        assertFalse(hasOnlyOverlappingMarkers(emptyList()))
        assertFalse(hasOnlyOverlappingMarkers(listOf(shop("first"))))
    }

    @Test
    fun `첫 매장에 가까워도 양 끝 매장이 1미터보다 멀면 팝업 대상이 아니다`() {
        assertFalse(
            hasOnlyOverlappingMarkers(
                listOf(
                    shop("first"),
                    shop("second", latitudeOffset = -0.000005),
                    shop("third", latitudeOffset = 0.000005),
                ),
            ),
        )
    }

    private fun shop(
        id: String,
        latitudeOffset: Double = 0.0,
    ) = RamenShop(
        id = id,
        name = id,
        address = "서울",
        location = Location(lat = 37.551 + latitudeOffset, lng = 126.921),
        kakaoPlaceUrl = null,
        instagramUrl = null,
        menuCategories = MenuCategories(listOf(Category.SHOYU)),
        isVisible = true,
        createdAt = "",
        updatedAt = "",
    )
}
