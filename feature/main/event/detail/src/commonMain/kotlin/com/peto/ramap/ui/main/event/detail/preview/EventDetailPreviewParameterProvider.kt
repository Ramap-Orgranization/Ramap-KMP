package com.peto.ramap.ui.main.event.detail.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.event.ExternalParticipant
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.main.event.detail.contract.EventDetailLoadKey
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState

class EventDetailPreviewParameterProvider : PreviewParameterProvider<EventDetailUiState> {
    override val values: Sequence<EventDetailUiState> =
        sequenceOf(
            EventDetailUiState(
                event =
                    previewEvent(
                        type = ShopEventType.COLLAB,
                        title = "라멘 팝업 이벤트",
                        description = "맛있는 라멘 팝업 이벤트입니다. 많은 참여 부탁드립니다.",
                        collaboratorShops = listOf(previewShop("shop2", "콜라보 샵")),
                        waitingMethod = "현장 대기",
                        waitingUrl = "https://catchtable.co.kr/",
                        imageUrls =
                            listOf(
                                "https://picsum.photos",
                                "https://picsum.photos",
                                "https://picsum.photos",
                                "https://picsum.photos",
                            ),
                    ),
                isNotificationVisible = true,
                canChangeNotification = true,
            ),
            EventDetailUiState(
                event =
                    previewEvent(
                        type = ShopEventType.POPUP,
                        title = "셰프 초청 팝업",
                        externalParticipants =
                            listOf(
                                ExternalParticipant(
                                    name = "게스트 셰프",
                                    instagramUrl = "https://picsum.photos",
                                ),
                            ),
                        waitingMethod = null,
                        waitingUrl = null,
                    ),
            ),
            EventDetailUiState(
                event =
                    previewEvent(
                        type = ShopEventType.STORE_RENEWAL,
                        title = "이리에 라멘 리뉴얼 오픈",
                        description = "",
                        endDate = null,
                        imageUrls =
                            listOf(
                                "https://picsum.photos",
                                "https://picsum.photos",
                                "https://picsum.photos",
                            ),
                    ),
            ),
            EventDetailUiState(
                event =
                    previewEvent(
                        type = ShopEventType.LIMITED_MENU,
                        title = "시즌 한정 시오라멘",
                        description = "",
                        endDate = null,
                        sourceUrl = "invalid-url",
                        isToday = false,
                        waitingMethod = null,
                        waitingUrl = null,
                        imageUrls = listOf("https://picsum.photos"),
                    ),
            ),
            EventDetailUiState(
                event =
                    previewEvent(
                        type = ShopEventType.SUMMER_LIMITED,
                        title = "냉유자 츠케멘",
                        isCancelledToday = true,
                    ),
                isNotificationVisible = true,
                canChangeNotification = true,
                isNotificationEnabled = true,
            ),
            EventDetailUiState(loadState = LoadState.loading(EventDetailLoadKey.Fetch)),
            EventDetailUiState(hasEventLoadFailed = true),
        )

    private fun previewEvent(
        type: ShopEventType,
        title: String,
        description: String = "이벤트 상세 설명입니다.",
        startDate: String = "2026-08-12",
        endDate: String? = "2026-08-16",
        sourceUrl: String = "https://www.instagram.com/ramap_official/",
        isToday: Boolean = true,
        collaboratorShops: List<RamenShop> = emptyList(),
        externalParticipants: List<ExternalParticipant> = emptyList(),
        waitingMethod: String? = "현장 대기",
        waitingUrl: String? = "https://catchtable.co.kr/",
        isCancelledToday: Boolean = false,
        imageUrls: List<String> = emptyList(),
    ) = ShopEvent(
        id = type.name,
        type = type,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        sourceUrl = sourceUrl,
        isToday = isToday,
        isVenue = true,
        venueShop = previewShop("shop1", "이리에 라멘"),
        collaboratorShops = collaboratorShops,
        externalParticipants = externalParticipants,
        waitingMethod = waitingMethod,
        waitingUrl = waitingUrl,
        isCancelledToday = isCancelledToday,
        imageUrls = imageUrls,
    )

    private fun previewShop(
        id: String,
        name: String,
    ): RamenShop =
        RamenShop(
            id = id,
            kakaoPlaceId = null,
            name = name,
            address = "서울시 마포구",
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
