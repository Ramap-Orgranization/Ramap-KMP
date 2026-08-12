package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

    @Test
    fun `모든 협업 매장과 외부 참여자를 도메인 이벤트로 변환한다`() {
        val event =
            response(eventType = "collab")
                .copy(
                    collaboratorShops =
                        listOf(
                            ramenShopResponseFixture(id = "partner-1"),
                            ramenShopResponseFixture(id = "partner-2"),
                        ),
                    externalParticipants =
                        listOf(
                            ExternalParticipantResponse(
                                name = "외부 셰프",
                                instagramUrl = "https://instagram.com/external",
                            ),
                        ),
                ).toDomain()

        assertEquals(listOf("partner-1", "partner-2"), event?.collaboratorShops?.map { it.id })
        assertEquals("외부 셰프", event?.externalParticipants?.single()?.name)
    }

    @Test
    fun `구버전 평면 응답도 도메인 이벤트로 변환한다`() {
        val response =
            Json.decodeFromString<ShopEventResponse>(
                """
                {
                  "id": "event",
                  "event_type": "summer_limited",
                  "title": "여름한정 메뉴",
                  "description": "설명",
                  "start_date": "2026-07-28",
                  "end_date": null,
                  "source_url": "https://instagram.com/p/event",
                  "is_today": true,
                  "is_venue": true,
                  "venue_shop_id": "shop",
                  "venue_shop_name": "566라멘",
                  "venue_address": "서울",
                  "venue_lat": 37.551,
                  "venue_lng": 126.921,
                  "venue_profile_image_url": "shop.jpg",
                  "cancelled_dates": [],
                  "is_cancelled_today": false
                }
                """,
            )

        val event = response.toDomain()

        assertEquals("shop", event?.venueShopId)
        assertEquals("566라멘", event?.venueShopName)
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
        venueShop = ramenShopResponseFixture(id = "shop", name = "566라멘"),
    )
}
