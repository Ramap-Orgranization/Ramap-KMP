package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.model.ShopEvent

interface RamenShopRepository {
    suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops>

    suspend fun fetchRamenShopsByIds(shopIds: Set<String>): RamapResult<RamenShops>

    suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?>

    suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>>

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamapResult<RamenShops>
}
