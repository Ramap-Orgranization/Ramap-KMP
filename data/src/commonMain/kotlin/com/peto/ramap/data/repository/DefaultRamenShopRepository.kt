package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.extension.toLocalDate
import com.peto.ramap.data.model.MenuResponse
import com.peto.ramap.data.model.MenuSectionResponse
import com.peto.ramap.data.model.ShopDetailResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.event.CalendarEventPage
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.menu.Menu
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.model.menu.Price
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.ShopDetail
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

    override suspend fun fetchShopDetail(shopId: String): RamapResult<ShopDetail> =
        invokeRequest {
            val response =
                dataSource.fetchShopDetail(shopId)
                    ?: error("매장 상세를 찾을 수 없습니다: $shopId")
            val menuUpdatedAt =
                response.menuSections
                    .takeIf(List<MenuSectionResponse>::isNotEmpty)
                    ?.let { dataSource.fetchShopMenuUpdatedAt(shopId) }
            shopDetail(response, shopId, menuUpdatedAt)
        }

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

    private fun shopDetail(
        response: ShopDetailResponse,
        shopId: String,
        menuUpdatedAt: String?,
    ): ShopDetail {
        val domainShop = response.shop.toDomain()
        val activeEvents = activeShopEvents(response.events)
        return ShopDetail(
            shop = domainShop,
            likeCount = response.likeCount,
            waitingSystem = response.waitingSystem?.toDomain(),
            event = activeShopEvent(activeEvents, response.eventParticipants, shopId),
            operatingNotice = response.operatingNotice?.toDomain(domainShop),
            menuSections = menuSections(response.menuSections, response.menuItems),
            menuUpdatedAt = menuUpdatedAt,
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

    private fun menuSections(
        sections: List<MenuSectionResponse>,
        items: List<MenuResponse>,
    ): List<MenuSection> {
        val itemsBySection = items.groupBy { it.sectionId }
        return sections
            .sortedWith(
                compareBy<MenuSectionResponse>(::menuSectionPriority)
                    .thenBy { it.displayOrder }
                    .thenBy { it.id },
            ).map { menuSection(it, itemsBySection[it.id].orEmpty()) }
            .filter { it.items.isNotEmpty() }
    }

    private fun menuSectionPriority(section: MenuSectionResponse): Int = if (section.title.trim() == PERMANENT_MENU_SECTION_TITLE) 1 else 0

    private fun menuSection(
        section: MenuSectionResponse,
        items: List<MenuResponse>,
    ): MenuSection =
        MenuSection(
            id = section.id,
            title = section.title,
            description = section.description,
            displayOrder = section.displayOrder,
            items = Menus(items.sortedBy { it.displayOrder }.map(::menuItem)),
        )

    private fun menuItem(item: MenuResponse): Menu =
        Menu(
            id = item.id,
            name = item.name,
            priceKrw = item.priceKrw?.let(::Price),
            priceText = item.priceText,
            description = item.description,
            imageUrl = item.imageUrl,
            sourceUrl = item.sourceUrl,
            displayOrder = item.displayOrder,
            isRepresentative = item.isRepresentative,
        )

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

    private companion object {
        const val PERMANENT_MENU_SECTION_TITLE = "상시메뉴"
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
