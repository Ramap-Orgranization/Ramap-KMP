package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.ShopDetail
import kotlinx.coroutines.delay

class FakeRamenShopRepository(
    private val result: RamenShops = RamenShops(emptyMap()),
    private val fetchByIdsResult: RamenShops = RamenShops(emptyMap()),
    private val searchResult: RamenShops = RamenShops(emptyMap()),
    var error: RamapError? = null,
    var activeEvent: ShopEvent? = null,
    private val activeEvents: List<ShopEvent> = emptyList(),
    private val searchDelayMillis: Long = 0,
    var activeEventError: RamapError? = null,
    var activeEventsError: RamapError? = null,
    var activeEventsDelayMillis: Long = 0,
    private val shopLikeCount: Long = 0L,
    var shopLikeCountError: RamapError? = null,
    var shopDetail: ShopDetail? = null,
    var shopDetailError: RamapError? = null,
) : RamenShopRepository {
    val requestedShopDetailIds = mutableListOf<String>()

    override suspend fun fetchShopDetail(shopId: String): RamapResult<ShopDetail> {
        requestedShopDetailIds += shopId
        return (shopDetailError ?: error)?.let { RamapResult.Error(it) }
            ?: shopDetail?.let { RamapResult.Success(it) }
            ?: RamapResult.Error(
                RamapError.Unknown(IllegalStateException("Missing fake shop detail")),
            )
    }

    val requestedActiveEventShopIds = mutableListOf<String>()

    override suspend fun fetchShopLikeCount(shopId: String): RamapResult<Long> = shopLikeCountError?.let { RamapResult.Error(it) } ?: RamapResult.Success(shopLikeCount)

    override suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?> {
        requestedActiveEventShopIds += shopId
        return (activeEventError ?: error)?.let { RamapResult.Error(it) } ?: RamapResult.Success(activeEvent)
    }

    var activeEventsRequestCount = 0
        private set

    override suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>> {
        activeEventsRequestCount += 1
        delay(activeEventsDelayMillis)
        return (activeEventsError ?: error)?.let { RamapResult.Error(it) } ?: RamapResult.Success(activeEvents)
    }

    override suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?> =
        (activeEventError ?: error)?.let { RamapResult.Error(it) }
            ?: RamapResult.Success(activeEvents.firstOrNull { it.id == eventId } ?: activeEvent)

    override suspend fun fetchEvent(eventId: String): RamapResult<ShopEvent?> =
        (activeEventError ?: error)?.let { RamapResult.Error(it) }
            ?: RamapResult.Success(
                activeEvents.firstOrNull { it.id == eventId }
                    ?: activeEvent,
            )

    val requestedBoundsHistory = mutableListOf<MapBounds>()
    val requestedShopIdsHistory = mutableListOf<Set<String>>()
    val requestedSearchQueries = mutableListOf<SearchQuery>()
    val requestedSearchLimits = mutableListOf<Int>()

    override suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops> {
        requestedBoundsHistory += bounds
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(result)
    }

    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
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
        delay(searchDelayMillis)
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(searchResult)
    }
}
