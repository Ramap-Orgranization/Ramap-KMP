package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.shop.ShopOverView

interface RamenShopRepository {
    suspend fun fetchShopOverview(shopId: String): RamapResult<ShopOverView>

    suspend fun fetchShopLikeCount(shopId: String): RamapResult<Long>

    suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops>

    suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops>

    suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?>

    suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>>

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

    /** 매장명 관련도와 [center] 기준 거리순으로 정렬한 지도 검색 결과를 반환한다. */
    suspend fun searchMapRamenShops(
        query: SearchQuery,
        center: Location?,
        limit: Int,
    ): RamapResult<RamenShops>
}
