package com.peto.ramap.domain.model.event

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopEventTest {
    @Test
    fun `이벤트 기간의 양 끝 날짜를 포함하고 잘못된 날짜를 거부한다`() {
        val event = event(startDate = "2024-02-28", endDate = "2024-03-01")

        assertTrue(event.occursOn(LocalDate(2024, 2, 28)))
        assertTrue(event.occursOn(LocalDate(2024, 3, 1)))
        assertFalse(event.occursOn(LocalDate(2024, 3, 2)))
        assertFalse(event(startDate = "2024-02-30", endDate = null).occursOn(LocalDate(2024, 2, 29)))
    }

    @Test
    fun `이벤트를 날짜별로 묶고 빈 날짜를 제외한다`() {
        val first = event(startDate = "2024-02-28", endDate = "2024-03-01")
        val second = event(startDate = "2024-03-01", endDate = "2024-03-01")

        val grouped =
            groupShopEventsByDate(
                dates =
                    listOf(
                        LocalDate(2024, 2, 27),
                        LocalDate(2024, 2, 28),
                        LocalDate(2024, 3, 1),
                    ),
                events = listOf(first, second),
            )

        assertFalse(grouped.containsKey(LocalDate(2024, 2, 27)))
        assertEquals(listOf(first), grouped[LocalDate(2024, 2, 28)])
        assertEquals(listOf(first, second), grouped[LocalDate(2024, 3, 1)])
    }

    @Test
    fun `취소된 날짜만 취소 상태로 판정한다`() {
        val event =
            event(
                startDate = "2024-02-28",
                endDate = "2024-03-01",
                cancelledDates = listOf(LocalDate(2024, 3, 1)),
            )

        assertFalse(event.isCancelledOn(LocalDate(2024, 2, 28)))
        assertTrue(event.isCancelledOn(LocalDate(2024, 3, 1)))
        assertFalse(event.isCancelledOn(LocalDate(2024, 3, 2)))
    }

    @Test
    fun `다가오는 단일 콜라보의 등록된 상대 매장을 찾는다`() {
        assertEquals(
            "라멘롱시즌",
            event(
                collaboratorShopId = "partner",
                collaboratorName = "라멘롱시즌",
                collaborationPartnerCount = 1,
            ).upcomingCollaborationPartnerName,
        )
        assertEquals(
            "요아케",
            event(
                isVenue = false,
                venueShopName = "요아케",
                collaborationPartnerCount = 1,
            ).upcomingCollaborationPartnerName,
        )
    }

    @Test
    fun `이벤트가 여러 개이거나 외부 상대면 상대 매장을 숨긴다`() {
        assertNull(
            event(
                collaboratorShopId = "partner",
                collaboratorName = "라멘롱시즌",
                activeEventCount = 2,
                collaborationPartnerCount = 1,
            ).upcomingCollaborationPartnerName,
        )
        assertNull(
            event(
                collaboratorShopId = "partner",
                collaboratorName = "라멘롱시즌",
                collaborationPartnerCount = 2,
            ).upcomingCollaborationPartnerName,
        )
        assertNull(
            event(collaboratorName = "외부 셰프", collaborationPartnerCount = 1).upcomingCollaborationPartnerName,
        )
    }

    private fun event(
        startDate: String = "2026-07-15",
        endDate: String? = "2026-07-15",
        isVenue: Boolean = true,
        venueShopName: String = "요아케",
        collaboratorShopId: String? = null,
        collaboratorName: String? = null,
        activeEventCount: Int = 1,
        collaborationPartnerCount: Int? = null,
        cancelledDates: List<LocalDate> = emptyList(),
    ) = ShopEvent(
        id = "event",
        type = ShopEventType.COLLAB,
        title = "title",
        description = "description",
        startDate = startDate,
        endDate = endDate,
        sourceUrl = "https://instagram.com/p/event",
        isToday = false,
        isVenue = isVenue,
        venueShopId = "shop",
        venueShopName = venueShopName,
        venueAddress = "address",
        collaboratorShopId = collaboratorShopId,
        collaboratorName = collaboratorName,
        collaboratorInstagramUrl = null,
        waitingMethod = null,
        waitingUrl = null,
        activeEventCount = activeEventCount,
        collaborationPartnerCount = collaborationPartnerCount,
        cancelledDates = cancelledDates,
    )
}
