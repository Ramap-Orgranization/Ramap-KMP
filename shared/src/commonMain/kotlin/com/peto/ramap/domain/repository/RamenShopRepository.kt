package com.peto.ramap.domain.repository

import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery

interface RamenShopRepository {
    suspend fun fetchRamenShops(bounds: MapBounds): RamenShops

    suspend fun fetchRamenShopsByIds(shopIds: Set<String>): RamenShops

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamenShops
}
