package com.peto.ramap.domain.model.shop

import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

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
    fun `현재 위치가 없으면 가장 가까운 매장은 없고 기존 매장 순서를 유지한다`() {
        val shops = RamenShops(listOf(ramenShopFixture()))

        assertNull(shops.nearestTo(null))
        assertSame(shops, shops.nearestFirstTo(null))
    }

    @Test
    fun `거리가 같은 매장은 기존 순서에서 첫 매장을 가장 가까운 매장으로 유지한다`() {
        val sharedLocation = Location(lat = 37.55, lng = 126.92)
        val firstShop = ramenShopFixture(id = "first-shop", location = sharedLocation)
        val secondShop = ramenShopFixture(id = "second-shop", location = sharedLocation)
        val shops = RamenShops(listOf(firstShop, secondShop))

        val nearest = shops.nearestTo(Location(lat = 37.5, lng = 126.9))
        val nearestFirst = shops.nearestFirstTo(Location(lat = 37.5, lng = 126.9))

        assertEquals(firstShop, nearest)
        assertEquals(listOf(firstShop, secondShop), nearestFirst.values.toList())
    }

    @Test
    fun `매장 목록이 비어 있으면 null을 반환한다`() {
        val result = RamenShops(emptyMap()).nearestTo(Location(lat = 37.55, lng = 126.92))

        assertNull(result)
    }
}
