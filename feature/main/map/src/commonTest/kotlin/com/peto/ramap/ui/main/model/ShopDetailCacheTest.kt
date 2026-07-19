package com.peto.ramap.ui.main.model

import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import com.peto.ramap.ui.main.map.model.ShopDetail
import com.peto.ramap.ui.main.map.model.ShopId
import com.peto.ramap.ui.main.map.model.cache.ShopDetailCache
import com.peto.ramap.ui.main.map.model.cache.ShopDetailCacheLookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ShopDetailCacheTest {
    @Test
    fun `저장하지 않은 매장은 캐시 미스로 조회한다`() {
        val cache = ShopDetailCache()

        val lookup = cache.find(ShopId("missing-shop"))

        assertIs<ShopDetailCacheLookup.Miss>(lookup)
    }

    @Test
    fun `저장한 매장은 상세를 포함한 캐시 히트로 조회한다`() {
        val shop = ramenShopFixture()
        val detail =
            ShopDetail(
                shop = shop,
                waitingSystem = waitingSystemFixture(shop.id),
                event = null,
            )
        val cache = ShopDetailCache()
        cache.store(detail)

        val lookup = cache.find(ShopId(shop.id))

        assertEquals(detail, assertIs<ShopDetailCacheLookup.Hit>(lookup).detail)
    }
}
