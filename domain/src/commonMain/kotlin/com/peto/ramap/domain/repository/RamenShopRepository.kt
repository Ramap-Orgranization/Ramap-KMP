package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.event.ShopEvent

interface RamenShopRepository {
    suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops>

    suspend fun fetchRamenShopsByIds(shopIds: Set<String>): RamapResult<RamenShops>

    suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?>

    suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>>

    suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?>

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamapResult<RamenShops>
}
