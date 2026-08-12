package com.peto.ramap.ui.main.event.list.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.event.list.contract.EventsUiState

class EventsPreviewParameterProvider : PreviewParameterProvider<EventsUiState> {
    override val values: Sequence<EventsUiState> =
        sequenceOf(
            EventsUiState(
                ongoingEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "ongoing-1",
                                    title = "오늘 진행 중인 라멘 이벤트",
                                    type = ShopEventType.POPUP,
                                    isToday = true,
                                ),
                                previewEvent(
                                    id = "ongoing-2",
                                    title = "또 다른 진행 중인 라멘 이벤트",
                                    type = ShopEventType.COLLAB,
                                    isToday = true,
                                ),
                            ),
                        ),
                    ),
                upcomingEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "upcoming-1",
                                    title = "다음 주 예정된 라멘 이벤트",
                                    type = ShopEventType.POPUP,
                                    isToday = false,
                                ),
                            ),
                        ),
                    ),
            ),
            EventsUiState(
                summerLimitedEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "ongoing-2",
                                    title = "오늘 진행 중인 라멘 이벤트",
                                    type = ShopEventType.SUMMER_LIMITED,
                                    isToday = true,
                                ),
                            ),
                        ),
                    ),
            ),
            EventsUiState(
                ongoingEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "cancelled-today",
                                    title = "오늘 취소된 토리하나 규코츠 이벤트",
                                    type = ShopEventType.POPUP,
                                    isToday = true,
                                    isCancelledToday = true,
                                    venueShopName = "토리하나",
                                ),
                            ),
                        ),
                    ),
            ),
            EventsUiState(
                upcomingEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "upcoming-2",
                                    title = "다음 주 예정된 라멘 이벤트",
                                    type = ShopEventType.POPUP,
                                    isToday = false,
                                ),
                            ),
                        ),
                    ),
            ),
            EventsUiState(
                upcomingEvents =
                    listOf(
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "upcoming-multiple-1",
                                    title = "규코츠라멘 이벤트",
                                    type = ShopEventType.POPUP,
                                    isToday = false,
                                    venueShopId = "preview-shop-1",
                                    venueShopName = "토리하나",
                                ),
                                previewEvent(
                                    id = "upcoming-multiple-2",
                                    title = "치킨멘 이벤트",
                                    type = ShopEventType.LIMITED_MENU,
                                    isToday = false,
                                    venueShopId = "preview-shop-1",
                                    venueShopName = "토리하나",
                                ),
                            ),
                        ),
                        ShopEvents(
                            listOf(
                                previewEvent(
                                    id = "upcoming-multiple-3",
                                    title = "냉라멘 이벤트",
                                    type = ShopEventType.LIMITED_MENU,
                                    isToday = false,
                                    venueShopId = "preview-shop-2",
                                    venueShopName = "니시무라멘 연남본점",
                                ),
                            ),
                        ),
                    ),
            ),
            EventsUiState(),
        )

    private fun previewEvent(
        id: String,
        title: String,
        type: ShopEventType,
        isToday: Boolean,
        venueShopId: String = "preview-shop",
        venueShopName: String = "라멘 프리뷰 매장",
        isCancelledToday: Boolean = false,
    ) = ShopEvent(
        id = id,
        type = type,
        title = title,
        description = "이벤트 설명",
        startDate = if (isToday) "2026-07-29" else "2026-08-05",
        endDate = if (isToday) "2026-07-31" else "2026-08-07",
        sourceUrl = "https://instagram.com/event",
        isToday = isToday,
        isVenue = true,
        venueShop = previewShop(venueShopId, venueShopName),
        waitingMethod = null,
        waitingUrl = null,
        isCancelledToday = isCancelledToday,
    )

    private fun previewShop(
        id: String,
        name: String,
    ) = RamenShop(
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
}
