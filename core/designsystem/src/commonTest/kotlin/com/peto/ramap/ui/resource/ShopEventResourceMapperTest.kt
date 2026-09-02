package com.peto.ramap.ui.resource

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_cancelled_notice
import ramap.shared.generated.resources.event_date_event
import ramap.shared.generated.resources.event_date_new_menu
import ramap.shared.generated.resources.event_date_store_renewal
import ramap.shared.generated.resources.event_detail_new_menu_title
import ramap.shared.generated.resources.event_detail_store_renewal_title
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.event_new_menu_venue
import ramap.shared.generated.resources.event_sold_out_notice
import ramap.shared.generated.resources.event_status_cancelled
import ramap.shared.generated.resources.event_status_sold_out
import ramap.shared.generated.resources.event_status_today
import ramap.shared.generated.resources.event_status_upcoming
import ramap.shared.generated.resources.event_store_renewal_venue
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_new_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_type_store_renewal
import ramap.shared.generated.resources.event_type_summer_limited
import ramap.shared.generated.resources.event_venue
import ramap.shared.generated.resources.shop_event_notice_collab_participant_today
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming_with_shop
import ramap.shared.generated.resources.shop_event_notice_limited_menu_today
import ramap.shared.generated.resources.shop_event_notice_limited_menu_upcoming
import ramap.shared.generated.resources.shop_event_notice_new_menu_today
import ramap.shared.generated.resources.shop_event_notice_participant_today
import ramap.shared.generated.resources.shop_event_notice_store_renewal_today
import ramap.shared.generated.resources.shop_event_notice_store_renewal_upcoming
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopEventResourceMapperTest {
    @Test
    fun `이벤트 타입에 맞는 상세 제목을 매핑한다`() {
        assertEquals(Res.string.event_detail_title, ShopEventResourceMapper.detailTitle(ShopEventType.COLLAB))
        assertEquals(
            Res.string.event_detail_new_menu_title,
            ShopEventResourceMapper.detailTitle(ShopEventType.NEW_MENU),
        )
        assertEquals(
            Res.string.event_detail_store_renewal_title,
            ShopEventResourceMapper.detailTitle(ShopEventType.STORE_RENEWAL),
        )
    }

    @Test
    fun `이벤트 타입에 맞는 장소 라벨을 매핑한다`() {
        assertEquals(Res.string.event_venue, ShopEventResourceMapper.venueTitle(ShopEventType.COLLAB))
        assertEquals(
            Res.string.event_new_menu_venue,
            ShopEventResourceMapper.venueTitle(ShopEventType.NEW_MENU),
        )
        assertEquals(
            Res.string.event_store_renewal_venue,
            ShopEventResourceMapper.venueTitle(ShopEventType.STORE_RENEWAL),
        )
    }

    @Test
    fun `이벤트 타입에 맞는 날짜 라벨을 매핑한다`() {
        assertEquals(Res.string.event_date_event, ShopEventResourceMapper.dateTitle(ShopEventType.COLLAB))
        assertEquals(Res.string.event_date_new_menu, ShopEventResourceMapper.dateTitle(ShopEventType.NEW_MENU))
        assertEquals(
            Res.string.event_date_store_renewal,
            ShopEventResourceMapper.dateTitle(ShopEventType.STORE_RENEWAL),
        )
    }

    @Test
    fun `신메뉴와 매장 리뉴얼은 시작일만 표시한다`() {
        assertEquals(
            "2026-07-23",
            ShopEventResourceMapper.displayEndDate(
                event(type = ShopEventType.NEW_MENU, endDate = "2026-07-29"),
            ),
        )
        assertEquals(
            "2026-07-23",
            ShopEventResourceMapper.displayEndDate(
                event(type = ShopEventType.STORE_RENEWAL, endDate = "2026-07-29"),
            ),
        )
        assertEquals(
            "2026-07-29",
            ShopEventResourceMapper.displayEndDate(
                event(type = ShopEventType.LIMITED_MENU, endDate = "2026-07-29"),
            ),
        )
    }

    @Test
    fun `이벤트 상태와 타입을 리소스로 매핑한다`() {
        assertEquals(Res.string.event_status_today, ShopEventResourceMapper.dateLabel(event(isToday = true)))
        assertEquals(Res.string.event_status_upcoming, ShopEventResourceMapper.dateLabel(event(isToday = false)))
        assertEquals(
            Res.string.event_status_today,
            ShopEventResourceMapper.dateLabel(
                event(type = ShopEventType.NEW_MENU, isToday = true, isStartDateToday = true),
            ),
        )
        assertEquals(
            Res.string.event_status_today,
            ShopEventResourceMapper.dateLabel(
                event(type = ShopEventType.STORE_RENEWAL, isToday = true, isStartDateToday = true),
            ),
        )
        assertEquals(
            null,
            ShopEventResourceMapper.dateLabel(event(type = ShopEventType.NEW_MENU, isToday = true)),
        )
        assertEquals(
            null,
            ShopEventResourceMapper.dateLabel(event(type = ShopEventType.STORE_RENEWAL, isToday = true)),
        )
        assertEquals(
            Res.string.event_status_cancelled,
            ShopEventResourceMapper.dateLabel(event(isToday = true, isCancelledToday = true)),
        )
        assertEquals(
            Res.string.event_status_sold_out,
            ShopEventResourceMapper.dateLabel(event(isToday = true, isSoldOutToday = true)),
        )
        assertEquals(Res.string.event_type_collab, ShopEventResourceMapper.typeLabel(ShopEventType.COLLAB))
        assertEquals(Res.string.event_type_popup, ShopEventResourceMapper.typeLabel(ShopEventType.POPUP))
        assertEquals(
            Res.string.event_type_limited_menu,
            ShopEventResourceMapper.typeLabel(ShopEventType.LIMITED_MENU),
        )
        assertEquals(
            Res.string.event_type_summer_limited,
            ShopEventResourceMapper.typeLabel(ShopEventType.SUMMER_LIMITED),
        )
        assertEquals(
            Res.string.event_type_new_menu,
            ShopEventResourceMapper.typeLabel(ShopEventType.NEW_MENU),
        )
        assertEquals(
            Res.string.event_type_store_renewal,
            ShopEventResourceMapper.typeLabel(ShopEventType.STORE_RENEWAL),
        )
    }

    @Test
    fun `매장과 참여자 이벤트 안내 문구의 리소스와 인자를 매핑한다`() {
        assertEquals(
            UiText(Res.string.shop_event_notice_limited_menu_upcoming),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.LIMITED_MENU, isToday = false, isVenue = true),
            ),
        )
        assertEquals(
            UiText(Res.string.shop_event_notice_collab_participant_today, listOf(VENUE_NAME)),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.COLLAB, isToday = true, isVenue = false),
            ),
        )
        assertEquals(
            UiText(Res.string.shop_event_notice_participant_today, listOf(VENUE_NAME)),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.POPUP, isToday = true, isVenue = false),
            ),
        )
    }

    @Test
    fun `중장기 한정 메뉴는 오늘 안내 문구를 숨긴다`() {
        assertEquals(
            null,
            ShopEventResourceMapper.notice(
                event(
                    type = ShopEventType.LIMITED_MENU,
                    endDate = "2026-07-29",
                    isToday = true,
                ),
            ),
        )
        assertEquals(
            UiText(Res.string.shop_event_notice_limited_menu_today),
            ShopEventResourceMapper.notice(
                event(
                    type = ShopEventType.SUMMER_LIMITED,
                    endDate = "2026-07-23",
                    isToday = true,
                ),
            ),
        )
    }

    @Test
    fun `예정된 단독 협업은 파트너 이름을 포함한다`() {
        val notice =
            ShopEventResourceMapper.notice(
                event(
                    type = ShopEventType.COLLAB,
                    isToday = false,
                    isVenue = true,
                    collaboratorShopId = "partner-id",
                    collaboratorName = PARTNER_NAME,
                    activeEventCount = 1,
                    collaborationPartnerCount = 1,
                ),
            )

        assertEquals(
            UiText(Res.string.shop_event_notice_collab_upcoming_with_shop, listOf(PARTNER_NAME)),
            notice,
        )
    }

    @Test
    fun `매장 리뉴얼 안내 문구와 타입을 매핑한다`() {
        assertEquals(
            UiText(Res.string.shop_event_notice_store_renewal_today),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.STORE_RENEWAL, isToday = true, isStartDateToday = true),
            ),
        )
        assertEquals(
            null,
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.STORE_RENEWAL, isToday = true),
            ),
        )
        assertEquals(
            UiText(Res.string.shop_event_notice_store_renewal_upcoming),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.STORE_RENEWAL, isToday = false),
            ),
        )
    }

    @Test
    fun `진행 중인 취소 리뉴얼은 취소 안내를 반환한다`() {
        assertEquals(
            UiText(Res.string.event_cancelled_notice),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.STORE_RENEWAL, isToday = true, isCancelledToday = true),
            ),
        )
    }

    @Test
    fun `진행 중인 품절 리뉴얼은 품절 안내를 반환한다`() {
        assertEquals(
            UiText(Res.string.event_sold_out_notice),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.STORE_RENEWAL, isToday = true, isSoldOutToday = true),
            ),
        )
    }

    @Test
    fun `신메뉴 안내 문구와 타입을 매핑한다`() {
        assertEquals(
            UiText(Res.string.shop_event_notice_new_menu_today),
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.NEW_MENU, isToday = true, isStartDateToday = true),
            ),
        )
        assertEquals(
            null,
            ShopEventResourceMapper.notice(
                event(type = ShopEventType.NEW_MENU, isToday = true),
            ),
        )
    }

    private fun event(
        type: ShopEventType = ShopEventType.POPUP,
        startDate: String = "2026-07-23",
        endDate: String? = "2026-07-23",
        isToday: Boolean = false,
        isVenue: Boolean = true,
        collaboratorShopId: String? = null,
        collaboratorName: String? = null,
        activeEventCount: Int = 1,
        collaborationPartnerCount: Int? = null,
        isCancelledToday: Boolean = false,
        isSoldOutToday: Boolean = false,
        isStartDateToday: Boolean = false,
    ) = ShopEvent(
        id = "event-id",
        type = type,
        title = "title",
        description = "description",
        startDate = startDate,
        endDate = endDate,
        sourceUrl = "https://example.com/event",
        isToday = isToday,
        isVenue = isVenue,
        venueShop = shop("venue-id", VENUE_NAME),
        collaboratorShops =
            collaboratorShopId
                ?.let { listOf(shop(it, collaboratorName.orEmpty())) }
                .orEmpty(),
        externalParticipants =
            if (collaboratorShopId == null && collaboratorName != null) {
                listOf(
                    com.peto.ramap.domain.model.event.ExternalParticipant(
                        collaboratorName,
                        "",
                    ),
                )
            } else {
                emptyList()
            },
        waitingMethod = null,
        waitingUrl = null,
        activeEventCount = activeEventCount,
        collaborationPartnerCount = collaborationPartnerCount,
        isCancelledToday = isCancelledToday,
        isSoldOutToday = isSoldOutToday,
        isStartDateToday = isStartDateToday,
    )

    private companion object {
        const val VENUE_NAME = "행사 매장"
        const val PARTNER_NAME = "협업 매장"
    }

    private fun shop(
        id: String,
        name: String,
    ): RamenShop =
        RamenShop(
            id = id,
            kakaoPlaceId = null,
            name = name,
            address = "address",
            location = Location(37.5, 127.0),
            kakaoPlaceUrl = null,
            naverPlaceUrl = null,
            phone = null,
            instagramUrl = null,
            menuCategories = MenuCategories(emptyList()),
            isVisible = true,
            createdAt = "",
            updatedAt = "",
        )
}
