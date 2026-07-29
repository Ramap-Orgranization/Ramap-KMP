package com.peto.ramap.ui.main.event.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_apple
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.shop_detail_link_apple_maps
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map

@Composable
internal fun EventMapActions(
    event: ShopEvent,
    onKakaoClick: (String) -> Unit,
    onNaverClick: (String) -> Unit,
    onAppleClick: (Double, Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        event.venueKakaoPlaceUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { url ->
            EventMapLink(
                icon = Res.drawable.kakao_map_icon,
                label = stringResource(Res.string.shop_detail_link_kakao_map),
                onClick = { onKakaoClick(url) },
            )
        }
        event.venueNaverPlaceUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { url ->
            EventMapLink(
                icon = Res.drawable.naver_map_icon,
                label = stringResource(Res.string.shop_detail_link_naver_map),
                onClick = { onNaverClick(url) },
            )
        }
        val latitude = event.venueLatitude
        val longitude = event.venueLongitude
        if (
            ExternalUriOpener.isAppleMapsAvailable &&
            latitude != null &&
            longitude != null
        ) {
            EventMapLink(
                icon = Res.drawable.ic_apple,
                label = stringResource(Res.string.shop_detail_link_apple_maps),
                onClick = { onAppleClick(latitude, longitude) },
            )
        }
    }
}

@Preview
@Composable
private fun EventMapActionsPreview() {
    RamapTheme {
        EventMapActions(
            event =
                ShopEvent(
                    id = "1",
                    type = ShopEventType.LIMITED_MENU,
                    title = "팝업 이벤트",
                    description = "이벤트 상세 설명입니다.",
                    startDate = "2024-07-01",
                    endDate = "2024-07-31",
                    sourceUrl = "https://instagram.com/p/123",
                    isToday = true,
                    isVenue = true,
                    venueShopId = "shop1",
                    venueShopName = "라멘야",
                    venueAddress = "서울시 마포구 어딘가",
                    venueKakaoPlaceUrl = "https://place.map.kakao.com/123",
                    venueNaverPlaceUrl = "https://map.naver.com/v5/entry/place/123",
                    venueLatitude = 37.5665,
                    venueLongitude = 126.9780,
                    collaboratorShopId = null,
                    collaboratorName = null,
                    collaboratorInstagramUrl = null,
                    waitingMethod = null,
                    waitingUrl = null,
                ),
            onKakaoClick = {},
            onNaverClick = {},
            onAppleClick = { _, _ -> },
        )
    }
}
