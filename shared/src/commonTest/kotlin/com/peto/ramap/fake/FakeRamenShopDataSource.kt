package com.peto.ramap.fake

import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.SearchQuery

class FakeRamenShopDataSource(
    private val responses: List<RamenShopResponse> = emptyList(),
    private val searchResponses: List<RamenShopResponse> = emptyList(),
    private val fetchByIdsResponses: List<RamenShopResponse> = emptyList(),
    private val error: Throwable? = null,
) : RamenShopDataSource {
    var requestedBounds: MapBounds? = null
        private set
    val requestedBoundsHistory = mutableListOf<MapBounds>()

    var requestedShopIds: Set<String>? = null
        private set

    var requestedSearchQuery: SearchQuery? = null
        private set

    var requestedSearchLimit: Int? = null
        private set

    override suspend fun fetchRamenShops(bounds: MapBounds): List<RamenShopResponse> {
        requestedBounds = bounds
        requestedBoundsHistory += bounds
        error?.let { throw it }
        return responses
    }

    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse> {
        requestedShopIds = shopIds
        error?.let { throw it }
        return fetchByIdsResponses
    }

    override suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): List<RamenShopResponse> {
        requestedSearchQuery = query
        requestedSearchLimit = limit
        error?.let { throw it }
        return searchResponses
    }
}
