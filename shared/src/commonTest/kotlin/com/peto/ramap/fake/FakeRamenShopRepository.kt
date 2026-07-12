package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository

class FakeRamenShopRepository(
    private val result: RamenShops = RamenShops(emptyMap()),
    private val fetchByIdsResult: RamenShops = RamenShops(emptyMap()),
    private val searchResult: RamenShops = RamenShops(emptyMap()),
    private val error: RamapError? = null,
) : RamenShopRepository {
    val requestedBoundsHistory = mutableListOf<MapBounds>()
    val requestedShopIdsHistory = mutableListOf<Set<String>>()
    val requestedSearchQueries = mutableListOf<SearchQuery>()
    val requestedSearchLimits = mutableListOf<Int>()

    override suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops> {
        requestedBoundsHistory += bounds
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(result)
    }

    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): RamapResult<RamenShops> {
        requestedShopIdsHistory += shopIds
        val shops = if (fetchByIdsResult.isNotEmpty()) fetchByIdsResult else RamenShops(result + searchResult)
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(shops)
    }

    override suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamapResult<RamenShops> {
        requestedSearchQueries += query
        requestedSearchLimits += limit
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(searchResult)
    }
}
