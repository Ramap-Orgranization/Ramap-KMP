package com.peto.ramap.ui.main.event.list.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents

class EventSectionPreviewParameterProvider : PreviewParameterProvider<List<ShopEvents>> {
    override val values: Sequence<List<ShopEvents>> =
        sequenceOf(
            listOf(
                ShopEvents(
                    listOf(
                        previewEvent(
                            id = "ongoing-1",
                            title = "오늘 진행 중인 라멘 이벤트",
                            type = ShopEventType.POPUP,
                        ),
                        previewEvent(
                            id = "ongoing-2",
                            title = "또 다른 진행 중인 라멘 이벤트",
                            type = ShopEventType.COLLAB,
                        ),
                    ),
                ),
            ),
            listOf(
                ShopEvents(
                    listOf(
                        previewEvent(
                            id = "ongoing-single",
                            title = "오늘 진행 중인 라멘 이벤트",
                            type = ShopEventType.POPUP,
                        ),
                    ),
                ),
            ),
            listOf(
                ShopEvents(
                    listOf(
                        previewEvent(
                            id = "cancelled-today",
                            title = "오늘 취소된 토리하나 이벤트",
                            type = ShopEventType.POPUP,
                            isCancelledToday = true,
                        ),
                    ),
                ),
            ),
        )

    private fun previewEvent(
        id: String,
        title: String,
        type: ShopEventType,
        isCancelledToday: Boolean = false,
    ) = ShopEvent(
        id = id,
        type = type,
        title = title,
        description = "이벤트 설명",
        startDate = "2026-07-29",
        endDate = "2026-07-31",
        sourceUrl = "https://instagram.com/event",
        isToday = true,
        isVenue = true,
        venueShopId = "preview-shop",
        venueShopName = "라멘 프리뷰 매장",
        venueAddress = "서울 마포구",
        collaboratorShopId = null,
        collaboratorName = null,
        collaboratorInstagramUrl = null,
        waitingMethod = null,
        waitingUrl = null,
        isCancelledToday = isCancelledToday,
    )
}
