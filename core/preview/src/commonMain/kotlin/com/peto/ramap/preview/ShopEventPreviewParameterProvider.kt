package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
                venueShop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first(),
                waitingMethod = "현장 대기",
                waitingUrl = "https://catchtable.co.kr/",
            ),
        )
}
