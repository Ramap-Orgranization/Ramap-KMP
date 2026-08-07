package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEventType
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopEventResponseTest {
    @Test
    fun `여름 한정 타입을 도메인 이벤트로 변환한다`() {
        val event = response(eventType = "summer_limited").toDomain()

        assertEquals(ShopEventType.SUMMER_LIMITED, event?.type)
    }

    @Test
    fun `종료일이 없으면 도메인 이벤트에도 종료일 미정으로 변환한다`() {
        val event =
            response(eventType = "limited_menu", endDate = null).toDomain()

        assertEquals(null, event?.endDate)
    }

    private fun response(
        eventType: String,
        endDate: String? = "2026-07-29",
    ) = ShopEventResponse(
        id = "event",
        eventType = eventType,
        title = "여름한정 메뉴",
        description = "설명",
        startDate = "2026-07-28",
        endDate = endDate,
        sourceUrl = "https://instagram.com/p/event",
        isToday = false,
        isVenue = true,
        venueShopId = "shop",
        venueShopName = "566라멘",
        venueAddress = "서울",
        venueProfileImageUrl = null,
    )
}
