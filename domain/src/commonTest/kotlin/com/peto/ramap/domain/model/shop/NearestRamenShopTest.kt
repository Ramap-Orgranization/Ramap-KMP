package com.peto.ramap.domain.model.shop

import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NearestRamenShopTest {
    @Test
    fun `현재 위치가 있으면 가장 가까운 매장을 반환한다`() {
        val nearShop =
            ramenShopFixture(
                id = "near-shop",
                location = Location(lat = 37.551, lng = 126.921),
            )
        val farShop =
            ramenShopFixture(
                id = "far-shop",
                location = Location(lat = 37.65, lng = 127.05),
            )

        val result =
            RamenShops(listOf(farShop, nearShop)).nearestTo(
                Location(lat = 37.55, lng = 126.92),
            )

        assertEquals(nearShop, result)
    }

    @Test
    fun `현재 위치가 없으면 null을 반환한다`() {
        val result = RamenShops(listOf(ramenShopFixture())).nearestTo(null)

        assertNull(result)
    }

    @Test
    fun `매장 목록이 비어 있으면 null을 반환한다`() {
        val result = RamenShops(emptyMap()).nearestTo(Location(lat = 37.55, lng = 126.92))

        assertNull(result)
    }
}
