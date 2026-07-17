package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.network.execute.invokeRequest

class DefaultRamenShopRepository(
    private val dataSource: RamenShopDataSource,
) : RamenShopRepository {
    override suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?> =
        invokeRequest { dataSource.fetchActiveEvent(eventId)?.toDomain() }

    override suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>> =
        invokeRequest {
            dataSource.fetchActiveEvents().mapNotNull { it.toDomain() }
        }

    override suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            val events = dataSource.fetchActiveShopEvents(shopId)
            val event = events.firstOrNull()?.toDomain() ?: return@invokeRequest null
            if (events.size != 1 || event.type != ShopEventType.COLLAB || event.isToday) {
                return@invokeRequest event.copy(activeEventCount = events.size)
            }
            val participants = dataSource.fetchShopEventParticipants(event.id)
            val partnerCount =
                if (event.isVenue) {
                    participants.size
                } else {
                    1 + participants.count { it.shopId != shopId }
                }
            event.copy(activeEventCount = 1, collaborationPartnerCount = partnerCount)
        }

    override suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops> =
        invokeRequest {
            RamenShops(
                dataSource
                    .fetchRamenShops(bounds)
                    .map { it.toDomain() }
                    .associateBy { it.id },
            )
        }

    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): RamapResult<RamenShops> =
        invokeRequest {
            RamenShops(
                dataSource
                    .fetchRamenShopsByIds(shopIds)
                    .map { it.toDomain() }
                    .associateBy { it.id },
            )
        }

    override suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): RamapResult<RamenShops> =
        invokeRequest {
            RamenShops(
                dataSource
                    .searchRamenShops(query, limit)
                    .map { it.toDomain() }
                    .associateBy { it.id },
            )
        }
}
