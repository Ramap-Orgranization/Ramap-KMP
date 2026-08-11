package com.peto.ramap.ui.main.event.calendar.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarUiState
import com.peto.ramap.ui.main.event.calendar.model.CalendarMonth
import kotlinx.datetime.LocalDate

class EventCalendarPreviewParameterProvider : PreviewParameterProvider<EventCalendarUiState> {
    override val values: Sequence<EventCalendarUiState> =
        sequenceOf(
            EventCalendarUiState(
                month = CalendarMonth(2026, 8),
                events =
                    listOf(
                        previewEvent,
                    ),
                notificationDates =
                    listOf(
                        LocalDate(2026, 8, 14),
                        LocalDate(2026, 8, 15),
                    ),
                hasPreviousMonthEvents = true,
                hasNextMonthEvents = true,
            ),
        )

    private companion object {
        private val previewEvent =
            ShopEvent(
                id = "preview-event",
                type = ShopEventType.POPUP,
                title = "여름 한정 라멘 이벤트",
                description = "이벤트 설명",
                startDate = "2026-08-19",
                endDate = "2026-08-20",
                sourceUrl = "https://instagram.com/event",
                isToday = false,
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
}
