package com.peto.ramap.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopEventResponseTest {
    @Test
    fun `종료일이 없으면 도메인 이벤트에도 종료일 미정으로 변환한다`() {
        val event =
            ShopEventResponse(
                id = "event",
                eventType = "limited_menu",
                title = "여름한정 메뉴",
                description = "설명",
                startDate = "2026-07-28",
                endDate = null,
                sourceUrl = "https://instagram.com/p/event",
                isToday = false,
                isVenue = true,
                venueShopId = "shop",
                venueShopName = "566라멘",
                venueAddress = "서울",
                venueProfileImageUrl = null,
            ).toDomain()

        assertEquals(null, event?.endDate)
    }
}
