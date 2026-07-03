package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.SearchQuery

interface RamenShopDataSource {
    suspend fun fetchRamenShops(bounds: MapBounds): List<RamenShopResponse>

    suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse>

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): List<RamenShopResponse>
}
