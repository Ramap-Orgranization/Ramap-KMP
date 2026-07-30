package com.peto.ramap.ui.main.event.list.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.ui.main.event.list.contract.EventsUiState

class EventsPreviewParameterProvider : PreviewParameterProvider<EventsUiState> {
    override val values: Sequence<EventsUiState> =
        sequenceOf(
            EventsUiState(
                events =
                    listOf(
                        previewEvent(id = "ongoing-1", title = "오늘 진행 중인 라멘 이벤트", isToday = true),
                        previewEvent(id = "ongoing-2", title = "또 다른 진행 중인 라멘 이벤트", isToday = true),
                        previewEvent(id = "upcoming-1", title = "다음 주 예정된 라멘 이벤트", isToday = false),
                    ),
            ),
            EventsUiState(
                events =
                    listOf(
                        previewEvent(id = "ongoing-2", title = "오늘 진행 중인 라멘 이벤트", isToday = true),
                    ),
            ),
            EventsUiState(
                events =
                    listOf(
                        previewEvent(id = "upcoming-2", title = "다음 주 예정된 라멘 이벤트", isToday = false),
                    ),
            ),
            EventsUiState(),
        )

    private fun previewEvent(
        id: String,
        title: String,
        isToday: Boolean,
    ) = ShopEvent(
        id = id,
        type = ShopEventType.POPUP,
        title = title,
        description = "이벤트 설명",
        startDate = if (isToday) "2026-07-29" else "2026-08-05",
        endDate = if (isToday) "2026-07-31" else "2026-08-07",
        sourceUrl = "https://instagram.com/event",
        isToday = isToday,
        isVenue = true,
        venueShopId = "preview-shop",
        venueShopName = "라멘 프리뷰 매장",
        venueAddress = "서울 마포구",
        collaboratorShopId = null,
        collaboratorName = null,
        collaboratorInstagramUrl = null,
        waitingMethod = null,
        waitingUrl = null,
    )
}
