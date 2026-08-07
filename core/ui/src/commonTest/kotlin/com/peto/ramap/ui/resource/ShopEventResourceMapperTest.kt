package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.ui.resource.event.ShopEventResourceMapper
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_status_today
import ramap.shared.generated.resources.event_status_upcoming
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_type_summer_limited
import ramap.shared.generated.resources.shop_event_notice_collab_participant_today
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming_with_shop
import ramap.shared.generated.resources.shop_event_notice_limited_menu_upcoming
import ramap.shared.generated.resources.shop_event_notice_participant_today
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopEventResourceMapperTest {
    @Test
    fun `이벤트 상태와 타입을 리소스로 매핑한다`() {
        assertEquals(Res.string.event_status_today, ShopEventResourceMapper.dateLabel(event(isToday = true)))
        assertEquals(Res.string.event_status_upcoming, ShopEventResourceMapper.dateLabel(event(isToday = false)))
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

    @Suppress("LongParameterList")
    private fun event(
        type: ShopEventType = ShopEventType.POPUP,
        isToday: Boolean = false,
        isVenue: Boolean = true,
        collaboratorShopId: String? = null,
        collaboratorName: String? = null,
        activeEventCount: Int = 1,
        collaborationPartnerCount: Int? = null,
    ) = ShopEvent(
        id = "event-id",
        type = type,
        title = "title",
        description = "description",
        startDate = "2026-07-23",
        endDate = "2026-07-23",
        sourceUrl = "https://example.com/event",
        isToday = isToday,
        isVenue = isVenue,
        venueShopId = "venue-id",
        venueShopName = VENUE_NAME,
        venueAddress = "address",
        collaboratorShopId = collaboratorShopId,
        collaboratorName = collaboratorName,
        collaboratorInstagramUrl = null,
        waitingMethod = null,
        waitingUrl = null,
        activeEventCount = activeEventCount,
        collaborationPartnerCount = collaborationPartnerCount,
    )

    private companion object {
        const val VENUE_NAME = "행사 매장"
        const val PARTNER_NAME = "협업 매장"
    }
}
