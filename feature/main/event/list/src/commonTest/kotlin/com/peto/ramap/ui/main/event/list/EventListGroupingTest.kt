package com.peto.ramap.ui.main.event.list

import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.contract.mapEventsToUiState
import com.peto.ramap.ui.main.event.list.contract.partitionBySchedule
import com.peto.ramap.ui.main.event.list.contract.selectEventFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventListGroupingTest {
    @Test
    fun `오늘 진행 중인 이벤트와 예정 이벤트를 기존 순서대로 나눈다`() {
        val upcomingFirst = event(id = "upcoming-first", isToday = false)
        val ongoingFirst = event(id = "ongoing-first", isToday = true)
        val ongoingSecond = event(id = "ongoing-second", isToday = true)
        val upcomingSecond = event(id = "upcoming-second", isToday = false)

        val (ongoingEvents, upcomingEvents) =
            partitionBySchedule(listOf(upcomingFirst, ongoingFirst, ongoingSecond, upcomingSecond))

        assertEquals(listOf(ongoingFirst, ongoingSecond), ongoingEvents)
        assertEquals(listOf(upcomingFirst, upcomingSecond), upcomingEvents)
    }

    @Test
    fun `여름 한정 이벤트도 오늘 진행 중과 예정 목록에 함께 포함한다`() {
        val ongoingSummerLimited =
            event(
                id = "ongoing-summer-limited",
                isToday = true,
                type = ShopEventType.SUMMER_LIMITED,
            )
        val upcomingSummerLimited =
            event(
                id = "upcoming-summer-limited",
                isToday = false,
                type = ShopEventType.SUMMER_LIMITED,
            )
        val ongoing = event(id = "ongoing", isToday = true)
        val upcoming = event(id = "upcoming", isToday = false)

        val (ongoingEvents, upcomingEvents) =
            partitionBySchedule(
                listOf(ongoingSummerLimited, upcomingSummerLimited, ongoing, upcoming),
            )

        assertEquals(listOf(ongoingSummerLimited, ongoing), ongoingEvents)
        assertEquals(listOf(upcomingSummerLimited, upcoming), upcomingEvents)
    }

    @Test
    fun `오늘 취소된 이벤트는 진행 중 목록에서 제외하고 취소가 끝나면 다시 노출한다`() {
        val cancelledToday = event(id = "cancelled-today", isToday = true).copy(isCancelledToday = true)
        val ongoing = event(id = "ongoing", isToday = true)

        val (ongoingEvents, upcomingEvents) = partitionBySchedule(listOf(cancelledToday, ongoing))

        assertEquals(listOf(ongoing), ongoingEvents)
        assertEquals(emptyList(), upcomingEvents)
    }

    @Test
    fun `이벤트 필터는 최초 응답을 다시 조회하지 않고 타입별 오늘과 예정 목록을 만든다`() {
        val events =
            listOf(
                event("summer-today", true, ShopEventType.SUMMER_LIMITED),
                event("summer-upcoming", false, ShopEventType.SUMMER_LIMITED),
                event("event-today", true, ShopEventType.POPUP),
                event("new-menu-upcoming", false, ShopEventType.NEW_MENU),
                event("renewal-upcoming", false, ShopEventType.STORE_RENEWAL),
            )
        val state = mapEventsToUiState(EventsUiState(), events)

        val summerState = selectEventFilter(state, EventFilter.SUMMER_LIMITED)
        assertEquals(listOf("summer-today"), summerState.ongoingEvents.flatten().map { it.id })
        assertEquals(listOf("summer-upcoming"), summerState.upcomingEvents.flatten().map { it.id })

        val newMenuState = selectEventFilter(state, EventFilter.NEW_MENU)
        assertEquals(listOf("new-menu-upcoming"), newMenuState.upcomingEvents.flatten().map { it.id })

        val renewalState = selectEventFilter(state, EventFilter.STORE_RENEWAL)
        assertEquals(listOf("renewal-upcoming"), renewalState.upcomingEvents.flatten().map { it.id })
    }

    @Test
    fun `모든 매장을 유지하고 같은 매장 이벤트 개수를 계산한다`() {
        val firstShopFirst = event(id = "first-shop-first", isToday = false, venueShopId = "first-shop")
        val secondShop = event(id = "second-shop", isToday = false, venueShopId = "second-shop")
        val firstShopSecond = event(id = "first-shop-second", isToday = false, venueShopId = "first-shop")
        val thirdShop = event(id = "third-shop", isToday = false, venueShopId = "third-shop")
        val fourthShop = event(id = "fourth-shop", isToday = false, venueShopId = "fourth-shop")
        val firstShopThird = event(id = "first-shop-third", isToday = false, venueShopId = "first-shop")

        val groups =
            ShopEvents.groupByVenue(
                listOf(
                    firstShopFirst,
                    secondShop,
                    firstShopSecond,
                    thirdShop,
                    fourthShop,
                    firstShopThird,
                ),
            )

        assertEquals(4, groups.size)
        assertEquals(listOf(firstShopFirst, firstShopSecond, firstShopThird), groups.first().toList())
        assertEquals(firstShopFirst, groups.first().representativeEvent)
        assertEquals(3, groups.first().eventCount)
        assertTrue(groups.first().hasMultipleEvents)
    }

    @Test
    fun `조회된 최신 등록순을 유지한다`() {
        val newestEvent = event(id = "newest", isToday = false, startDate = "2026-08-01")
        val olderEvent = event(id = "older", isToday = false, startDate = "2026-08-10")

        val state =
            mapEventsToUiState(
                EventsUiState(selectedFilter = EventFilter.EVENT),
                listOf(newestEvent, olderEvent),
            )

        assertEquals(
            listOf(newestEvent, olderEvent),
            state.upcomingEvents.flatten(),
        )
    }

    private fun event(
        id: String,
        isToday: Boolean,
        type: ShopEventType = ShopEventType.POPUP,
        venueShopId: String = "shop",
        startDate: String = if (isToday) "2026-07-13" else "2026-07-15",
    ) = ShopEvent(
        id = id,
        type = type,
        title = id,
        description = "설명",
        startDate = startDate,
        endDate = if (isToday) "2026-07-14" else "2026-07-16",
        sourceUrl = "https://instagram.com/event",
        isToday = isToday,
        isVenue = true,
        venueShop = ramenShopFixture(id = venueShopId, name = "매장", address = "서울"),
        waitingMethod = null,
        waitingUrl = null,
    )
}
