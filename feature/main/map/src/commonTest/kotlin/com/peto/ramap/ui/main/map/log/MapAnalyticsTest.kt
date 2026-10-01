package com.peto.ramap.ui.main.map.log

import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.ui.main.map.log.event.ShopMapLinkOpened
import kotlin.test.Test
import kotlin.test.assertEquals

class MapAnalyticsTest {
    @Test
    fun shopMapLinkOpened_containsShopAndProviderParameters() {
        val event =
            ShopMapLinkOpened(
                shopId = "shop-1",
                shopName = "라멘집",
                mapProvider = "naver",
            )

        assertEquals("shop_map_link_open", event.name)
        assertEquals(
            mapOf(
                "shop_id" to "shop-1",
                "shop_name" to "라멘집",
                "map_provider" to "naver",
            ),
            event.params(),
        )
    }

    @Test
    fun mapAnalytics_logsMapLinkEventOnce() {
        val tracker = FakeAnalyticsTracker()
        val analytics = MapAnalytics(tracker)
        val shop =
            com.peto.ramap.domain.model.shop.RamenShop(
                id = "shop-1",
                name = "라멘집",
                address = "서울",
                location =
                    com.peto.ramap.domain.model.shop
                        .Location(0.0, 0.0),
                kakaoPlaceUrl = null,
                instagramUrl = null,
                menuCategories =
                    com.peto.ramap.domain.model.shop
                        .MenuCategories(emptyList()),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
            )

        analytics.logShopMapLinkOpened(shop, "apple")

        assertEquals(1, tracker.events.size)
        assertEquals("shop_map_link_open", tracker.events.single().name)
    }
}
