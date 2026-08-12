package com.peto.ramap.ui.main.event.calendar.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
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
                title = "토리하나 규코츠 이벤트",
                description = "이벤트 설명",
                startDate = "2026-08-19",
                endDate = "2026-08-20",
                sourceUrl = "https://instagram.com/event",
                isToday = false,
                isVenue = true,
                venueShop = previewShop("preview-shop", "토리하나"),
                waitingMethod = null,
                waitingUrl = null,
                cancelledDates = listOf(LocalDate(2026, 8, 19)),
            )
    }
}

private fun previewShop(
    id: String,
    name: String,
): RamenShop =
    RamenShop(
        id = id,
        kakaoPlaceId = null,
        name = name,
        address = "서울 마포구",
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
