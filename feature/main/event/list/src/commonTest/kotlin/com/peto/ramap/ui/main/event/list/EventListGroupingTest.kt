package com.peto.ramap.ui.main.event.list

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.ui.main.event.list.partitionBySchedule
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `진행 중인 여름 한정 이벤트만 별도 목록으로 제외하고 예정된 여름 한정 이벤트는 예정 목록에 포함한다`() {
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

        assertEquals(listOf(ongoing), ongoingEvents)
        assertEquals(listOf(upcomingSummerLimited, upcoming), upcomingEvents)
    }

    private fun event(
        id: String,
        isToday: Boolean,
        type: ShopEventType = ShopEventType.POPUP,
    ) = ShopEvent(
        id = id,
        type = type,
        title = id,
        description = "설명",
        startDate = if (isToday) "2026-07-13" else "2026-07-15",
        endDate = if (isToday) "2026-07-14" else "2026-07-16",
        sourceUrl = "https://instagram.com/event",
        isToday = isToday,
        isVenue = true,
        venueShopId = "shop",
        venueShopName = "매장",
        venueAddress = "서울",
        collaboratorShopId = null,
        collaboratorName = null,
        collaboratorInstagramUrl = null,
        waitingMethod = null,
        waitingUrl = null,
    )
}
