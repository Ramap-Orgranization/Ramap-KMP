package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.EventVenue
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType

class ShopEventPreviewParameterProvider : PreviewParameterProvider<ShopEvent> {
    override val values: Sequence<ShopEvent> =
        sequenceOf(
            ShopEvent(
                id = "preview-event",
                type = ShopEventType.POPUP,
                title = "셰프 초청 팝업",
                description = "특별한 팝업 이벤트입니다.",
                startDate = "2026-08-12",
                endDate = "2026-08-16",
                sourceUrl = "https://www.instagram.com/ramap_official/",
                isToday = true,
                isVenue = true,
                venue = EventVenue.Registered(RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first()),
                waitingMethod = "현장 대기",
                waitingUrl = "https://catchtable.co.kr/",
            ),
            ShopEvent(
                id = "map-only-venue-preview-event",
                type = ShopEventType.POPUP,
                title = "지도 장소 팝업",
                description = "특별한 팝업 이벤트입니다.",
                startDate = "2026-08-12",
                endDate = "2026-08-16",
                sourceUrl = "https://www.instagram.com/ramap_official/",
                isToday = true,
                isVenue = true,
                venue = EventVenue.External(id = null, name = "라멘 페스티벌", naverMapUrl = "https://map.naver.com/p/entry/place/1"),
                waitingMethod = "현장 대기",
                waitingUrl = "https://catchtable.co.kr/",
            ),
            ShopEvent(
                id = "name-only-venue-preview-event",
                type = ShopEventType.POPUP,
                title = "장소명 팝업",
                description = "특별한 팝업 이벤트입니다.",
                startDate = "2026-08-12",
                endDate = "2026-08-16",
                sourceUrl = "https://www.instagram.com/ramap_official/",
                isToday = true,
                isVenue = true,
                venue = EventVenue.External(id = null, name = "라멘 페스티벌"),
                waitingMethod = "현장 대기",
                waitingUrl = "https://catchtable.co.kr/",
            ),
            ShopEvent(
                id = "external-venue-preview-event",
                type = ShopEventType.POPUP,
                title = "라멘 페스티벌 팝업",
                description = "특별한 팝업 이벤트입니다.",
                startDate = "2026-08-12",
                endDate = "2026-08-16",
                sourceUrl = "https://www.instagram.com/ramap_official/",
                isToday = true,
                isVenue = true,
                venue =
                    EventVenue.External(
                        id = null,
                        name = "라멘 페스티벌",
                        instagramUrl = "https://www.instagram.com/ramen_festival/",
                        naverMapUrl = "https://map.naver.com/p/entry/place/1",
                        kakaoMapUrl = "https://place.map.kakao.com/1",
                    ),
                waitingMethod = "현장 대기",
                waitingUrl = "https://catchtable.co.kr/",
            ),
        )
}
