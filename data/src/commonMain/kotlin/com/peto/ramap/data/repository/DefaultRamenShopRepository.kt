package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.extension.toLocalDate
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.data.model.ShopOverviewResponse
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultRamenShopRepository(
    private val dataSource: RamenShopDataSource,
) : RamenShopRepository {
    override suspend fun fetchShopOverview(shopId: String): RamapResult<ShopOverView> =
        invokeRequest {
            val response = dataSource.fetchShopOverview(shopId)
            shopOverview(response, shopId)
        }

    override suspend fun fetchShopLikeCount(shopId: String): RamapResult<Long> = invokeRequest { dataSource.fetchShopOverview(shopId).likeCount }

    override suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            dataSource
                .fetchEvents(eventId)
                .singleOrNull()
                ?.let(::toDomain)
                ?.takeIf(::isVisibleInActiveEvents)
        }

    override suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>> =
        invokeRequest {
            dataSource
                .fetchEvents(eventId = null)
                .map(::toDomain)
                .filter(::isVisibleInActiveEvents)
        }

    override suspend fun fetchEvent(eventId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            dataSource.fetchEvents(eventId).singleOrNull()?.let(::toDomain)
        }

    override suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            val events = activeShopEvents(dataSource.fetchActiveShopEvents(shopId))
            val event = events.firstOrNull() ?: return@invokeRequest null
            val participants =
                if (needsCollaborationParticipants(events)) {
                    dataSource.fetchShopEventParticipants(event.id)
                } else {
                    emptyList()
                }
            activeShopEvent(events, participants, shopId)
        }

    private fun shopOverview(
        response: ShopOverviewResponse,
        shopId: String,
    ): ShopOverView {
        val domainShop = response.shop.toDomain()
        val activeEvents = activeShopEvents(response.events)
        return ShopOverView(
            shop = domainShop,
            likeCount = response.likeCount,
            waitingSystem = response.shop.toWaitingSystem(),
            event = activeShopEvent(activeEvents, response.eventParticipants, shopId),
            operatingNotice = response.operatingNotice?.toDomain(domainShop),
        )
    }

    private fun activeShopEvents(responses: List<ShopEventResponse>): List<ShopEvent> =
        responses
            .map(::toDomain)
            .filter(::isVisibleInActiveEvents)
            .filterNot(::isGenericOngoingRenewal)

    private fun needsCollaborationParticipants(events: List<ShopEvent>): Boolean = events.size == 1 && events.single().type == ShopEventType.COLLAB && !events.single().isToday

    private fun activeShopEvent(
        events: List<ShopEvent>,
        participants: List<ShopEventParticipantResponse>,
        shopId: String,
    ): ShopEvent? {
        val event = events.firstOrNull() ?: return null
        if (!needsCollaborationParticipants(events)) return event.copy(activeEventCount = events.size)
        val eventParticipants = participants.filter { it.eventId == event.id }
        val partnerCount =
            if (event.isVenue) {
                eventParticipants.size
            } else {
                1 + eventParticipants.count { it.shopId != shopId }
            }
        return event.copy(activeEventCount = 1, collaborationPartnerCount = partnerCount)
    }

    private fun toDomain(response: ShopEventResponse): ShopEvent {
        val event = response.toDomain()
        val today = today()
        val isStartDateToday = event.startDate == today.toString()
        if (event.type != ShopEventType.STORE_RENEWAL) {
            return event.copy(isStartDateToday = isStartDateToday)
        }
        return event.copy(
            isToday = isRenewalOngoingOn(event.startDate, today),
            isStartDateToday = isStartDateToday,
        )
    }

    private fun isRenewalOngoingOn(
        startDate: String,
        today: LocalDate,
    ): Boolean {
        val start = startDate.toLocalDate()
        return today >= start && today < start.plus(1, DateTimeUnit.MONTH)
    }

    private fun isVisibleInActiveEvents(event: ShopEvent): Boolean {
        if (event.type != ShopEventType.STORE_RENEWAL) return true
        val startDate = event.startDate.toLocalDate()
        return today() < startDate.plus(1, DateTimeUnit.MONTH)
    }

    private fun isGenericOngoingRenewal(event: ShopEvent): Boolean =
        event.type == ShopEventType.STORE_RENEWAL &&
            event.isToday &&
            !event.isStartDateToday &&
            !event.isCancelledToday &&
            !event.isSoldOutToday

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.of(SEOUL_TIME_ZONE))

    override suspend fun fetchRamenShops(bounds: MapBounds): RamapResult<RamenShops> =
        invokeRequest {
            RamenShops(
                dataSource
                    .fetchRamenShops(bounds)
                    .map { it.toDomain() }
                    .associateBy { it.id },
            )
        }

    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> =
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

    override suspend fun searchMapRamenShops(
        query: SearchQuery,
        center: Location?,
        limit: Int,
    ): RamapResult<RamenShops> =
        invokeRequest {
            RamenShops(dataSource.searchMapRamenShops(query, center, limit).map { it.toDomain() })
        }

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
