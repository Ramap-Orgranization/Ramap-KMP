package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.SearchQuery

internal interface RamenShopDataSource {
    suspend fun fetchRamenShops(bounds: MapBounds): List<RamenShopResponse>

    suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse>

    suspend fun fetchActiveShopEvents(shopId: String): List<ShopEventResponse>

    suspend fun fetchActiveEvents(): List<ShopEventResponse>

    suspend fun fetchActiveEvent(eventId: String): ShopEventResponse?

    suspend fun fetchShopEventParticipants(eventId: String): List<ShopEventParticipantResponse>

    suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): List<RamenShopResponse>
}
