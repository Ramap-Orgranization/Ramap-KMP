package com.peto.ramap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopEventTest {
    @Test
    fun formatsSingleDayInKorean() {
        val event = event(startDate = "2026-07-15", endDate = "2026-07-15")

        assertEquals("2026년 7월 15일", event.formattedDate)
    }

    @Test
    fun formatsDateRangeInKorean() {
        val event = event(startDate = "2026-07-15", endDate = "2026-07-16")

        assertEquals("2026년 7월 15일 ~ 2026년 7월 16일", event.formattedDate)
    }

    @Test
    fun findsRegisteredPartnerForSingleUpcomingCollaboration() {
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
    fun hidesPartnerWhenMultipleEventsExistOrPartnerIsExternal() {
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
        endDate: String = "2026-07-15",
        isVenue: Boolean = true,
        venueShopName: String = "요아케",
        collaboratorShopId: String? = null,
        collaboratorName: String? = null,
        activeEventCount: Int = 1,
        collaborationPartnerCount: Int? = null,
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
    )
}
