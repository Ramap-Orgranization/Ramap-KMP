package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.datetime.LocalDate
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
    fun `매장 리뉴얼 타입은 종료일을 무시하고 시작일 하루 규칙을 사용한다`() {
        val event =
            response(eventType = "store_renewal", endDate = "2026-08-10")
                .toDomain()

        assertEquals(ShopEventType.STORE_RENEWAL, event?.type)
        assertEquals(null, event?.endDate)
    }

    @Test
    fun `이벤트 이미지 경로를 허가된 공개 Storage URL로 최대 다섯 장 매핑한다`() {
        val event =
            response(
                eventType = "popup",
                imagePaths =
                    listOf(
                        "events/event-1/1.png",
                        "https://untrusted.example/image.png",
                        "../private.png",
                        "events/event-1/2.png",
                        "events/event-1/3.png",
                        "events/event-1/4.png",
                        "events/event-1/5.png",
                        "events/event-1/6.png",
                    ),
            ).toDomain()

        assertEquals(5, event?.imageUrls?.size)
        assertEquals(
            "${com.peto.ramap.network.config.RamapSecrets.supabaseUrl.trimEnd(
                '/',
            )}/storage/v1/object/public/event-images/events/event-1/1.png",
            event?.imageUrls?.first(),
        )
        assertEquals(
            "${com.peto.ramap.network.config.RamapSecrets.supabaseUrl.trimEnd(
                '/',
            )}/storage/v1/object/public/event-images/events/event-1/5.png",
            event?.imageUrls?.last(),
        )
    }

    @Test
    fun `품절 날짜와 오늘 품절 상태를 도메인 이벤트로 변환한다`() {
        val event =
            response(eventType = "limited_menu")
                .copy(
                    soldOutDates = listOf("2026-07-29"),
                    isSoldOutToday = true,
                ).toDomain()

        assertEquals(listOf(LocalDate(2026, 7, 29)), event?.soldOutDates)
        assertEquals(true, event?.isSoldOutToday)
    }

    @Test
    fun `취소 사유와 선택적 출처 URL을 도메인 이벤트로 변환한다`() {
        val event =
            response(eventType = "limited_menu")
                .copy(
                    cancellationReason = "연휴 기간중 판매가 어렵습니다🙏",
                    cancellationSourceUrl = "https://www.instagram.com/p/cancellation/",
                ).toDomain()

        assertEquals("연휴 기간중 판매가 어렵습니다🙏", event?.cancellationReason)
        assertEquals("https://www.instagram.com/p/cancellation/", event?.cancellationSourceUrl)
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

    private fun response(
        eventType: String,
        endDate: String? = "2026-07-29",
        imagePaths: List<String> = emptyList(),
        soldOutDates: List<String> = emptyList(),
        isSoldOutToday: Boolean = false,
        cancellationReason: String? = null,
        cancellationSourceUrl: String? = null,
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
        imagePaths = imagePaths,
        soldOutDates = soldOutDates,
        isSoldOutToday = isSoldOutToday,
        cancellationReason = cancellationReason,
        cancellationSourceUrl = cancellationSourceUrl,
    )
}
