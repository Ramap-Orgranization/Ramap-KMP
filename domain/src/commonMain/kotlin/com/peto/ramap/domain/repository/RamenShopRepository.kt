package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.CalendarEventPage
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery

interface RamenShopRepository {
    suspend fun fetchShopLikeCount(shopId: String): RamapResult<Long>

    suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops>

    suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops>

    suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?>

    suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>>

    suspend fun fetchCalendarEvents(
        startDate: String,
        endDate: String,
    ): RamapResult<List<ShopEvent>>

    suspend fun fetchCalendarEventPage(monthStart: String): RamapResult<CalendarEventPage>

    fun invalidateCalendarEventPage(monthStart: String)

    /**
     * 딥링크로 들어왔을 때 이벤트가 없으면 [RamapResult.Success]에 `null`을 담고,
     * 요청 자체가 실패하면 [RamapResult.Error]를 반환한다.
     */
    suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?>

    suspend fun fetchEvent(eventId: String): RamapResult<ShopEvent?>

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamapResult<RamenShops>
}
