package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

internal fun eventSection(
    scope: LazyListScope,
    title: String,
    events: List<ShopEvent>,
    isOngoingSection: Boolean,
    horizontalContentPadding: Dp,
    onEventClick: (ShopEvent) -> Unit,
) {
    if (events.isEmpty()) return

    scope.item(key = title) {
        AppText(
            text = title,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 15.dp),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
    }
    if (isOngoingSection) {
        scope.item(key = "$title-events") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalContentPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(events, key = { event -> event.id }) { event ->
                    OngoingEventShopItem(
                        event = event,
                        onClick = { onEventClick(event) },
                    )
                }
            }
        }
    } else {
        scope.items(events, key = ShopEvent::id) { event ->
            EventCard(
                event = event,
                dateText = eventDateText(event.startDate, event.endDate),
                onClick = { onEventClick(event) },
                modifier = Modifier.padding(horizontal = horizontalContentPadding),
            )
        }
    }
}

@Composable
private fun OngoingEventShopItem(
    event: ShopEvent,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(72.dp)
                .noRippleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape),
        ) {
            RemoteShopImage(
                url = event.venueProfileImageUrl,
                modifier = Modifier.size(68.dp),
            )
        }
        AppText(
            text = event.venueShopName,
            modifier = Modifier.fillMaxWidth(),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OngoingEventShopItemPreview() {
    RamapTheme {
        OngoingEventShopItem(
            event =
                ShopEvent(
                    id = "ongoing-1",
                    type = ShopEventType.POPUP,
                    title = "오늘 진행 중인 라멘 이벤트",
                    description = "이벤트 설명",
                    startDate = "2026-07-29",
                    endDate = "2026-07-31",
                    sourceUrl = "https://instagram.com/event",
                    isToday = true,
                    isVenue = true,
                    venueShopId = "preview-shop",
                    venueShopName = "무진장이름이긴매장입니다.",
                    venueAddress = "서울 마포구",
                    collaboratorShopId = null,
                    collaboratorName = null,
                    collaboratorInstagramUrl = null,
                    waitingMethod = null,
                    waitingUrl = null,
                    venueProfileImageUrl = null,
                ),
            onClick = {},
        )
    }
}
