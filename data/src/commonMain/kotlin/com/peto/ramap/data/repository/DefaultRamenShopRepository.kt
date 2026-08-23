package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.extension.toLocalDate
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.event.CalendarEventPage
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
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
    private val calendarEventPageCache = mutableMapOf<String, CalendarEventPage>()

    override suspend fun fetchShopLikeCount(shopId: String): RamapResult<Long> =
        invokeRequest {
            dataSource.fetchShopLikeCount(shopId)
        }

    override suspend fun fetchActiveEvent(eventId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            dataSource.fetchActiveEvent(eventId)?.let(::toDomain)?.takeIf(::isVisibleInActiveEvents)
        }

    override suspend fun fetchActiveEvents(): RamapResult<List<ShopEvent>> =
        invokeRequest {
            dataSource
                .fetchActiveEvents()
                .map(::toDomain)
                .filter(::isVisibleInActiveEvents)
        }

    override suspend fun fetchCalendarEvents(
        startDate: String,
        endDate: String,
    ): RamapResult<List<ShopEvent>> =
        invokeRequest {
            dataSource.fetchCalendarEvents(startDate, endDate).map(::toDomain)
        }

    override suspend fun fetchCalendarEventPage(monthStart: String): RamapResult<CalendarEventPage> {
        calendarEventPageCache[monthStart]?.let { return RamapResult.Success(it) }

        val result =
            invokeRequest {
                val page = dataSource.fetchCalendarEventPage(monthStart)
                CalendarEventPage(
                    events = page.events.map(::toDomain),
                    hasPrevious = page.hasPrevious,
                    hasNext = page.hasNext,
                    notificationDates = page.notificationDates.map(String::toLocalDate),
                )
            }
        if (result is RamapResult.Success) {
            calendarEventPageCache[monthStart] = result.data
        }
        return result
    }

    override fun invalidateCalendarEventPage(monthStart: String) {
        calendarEventPageCache.remove(monthStart)
    }

    override suspend fun fetchEvent(eventId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            dataSource.fetchEvent(eventId)?.let(::toDomain)
        }

    override suspend fun fetchActiveShopEvent(shopId: String): RamapResult<ShopEvent?> =
        invokeRequest {
            val events =
                dataSource
                    .fetchActiveShopEvents(shopId)
                    .map(::toDomain)
                    .filter(::isVisibleInActiveEvents)
                    .filterNot(::isGenericOngoingRenewal)
            val event = events.firstOrNull() ?: return@invokeRequest null
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

    private fun toDomain(response: ShopEventResponse): ShopEvent {
        val event = response.toDomain()
        if (event.type != ShopEventType.STORE_RENEWAL) return event
        val today = today()
        return event.copy(
            isToday = isRenewalOngoingOn(event.startDate, today),
            isStartDateToday = event.startDate == today.toString(),
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

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
