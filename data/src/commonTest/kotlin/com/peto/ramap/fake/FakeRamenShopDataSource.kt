package com.peto.ramap.fake

import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.model.CalendarEventPageResponse
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.SearchQuery

internal class FakeRamenShopDataSource(
    private val responses: List<RamenShopResponse> = emptyList(),
    private val searchResponses: List<RamenShopResponse> = emptyList(),
    private val fetchByIdsResponses: List<RamenShopResponse> = emptyList(),
    private val activeEventResponses: List<ShopEventResponse> = emptyList(),
    private val activeEventsResponses: List<ShopEventResponse> = emptyList(),
    private val calendarEventsResponses: List<ShopEventResponse> = emptyList(),
    private val calendarEventPageResponse: CalendarEventPageResponse? = null,
    private val participantResponses: List<ShopEventParticipantResponse> = emptyList(),
    private val error: Throwable? = null,
) : RamenShopDataSource {
    override suspend fun fetchShopLikeCount(shopId: String): Long = 0L

    override suspend fun fetchActiveShopEvents(shopId: String): List<ShopEventResponse> = activeEventResponses

    override suspend fun fetchActiveEvents(): List<ShopEventResponse> = activeEventsResponses

    override suspend fun fetchActiveEvent(eventId: String): ShopEventResponse? = activeEventsResponses.firstOrNull { it.id == eventId }

    override suspend fun fetchCalendarEvents(
        startDate: String,
        endDate: String,
    ): List<ShopEventResponse> = calendarEventsResponses

    override suspend fun fetchCalendarEventPage(monthStart: String): CalendarEventPageResponse {
        error?.let { throw it }
        calendarEventPageRequestCount += 1
        return calendarEventPageResponse
            ?: CalendarEventPageResponse(events = calendarEventsResponses)
    }

    var calendarEventPageRequestCount: Int = 0
        private set

    override suspend fun fetchEvent(eventId: String): ShopEventResponse? =
        calendarEventsResponses.firstOrNull { it.id == eventId }
            ?: activeEventsResponses.firstOrNull { it.id == eventId }

    override suspend fun fetchShopEventParticipants(eventId: String): List<ShopEventParticipantResponse> = participantResponses

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
