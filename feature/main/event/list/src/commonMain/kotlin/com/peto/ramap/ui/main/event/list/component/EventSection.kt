package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.event.list.preview.EventSectionPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_additional_events
import ramap.shared.generated.resources.event_list_ongoing_section
import ramap.shared.generated.resources.event_status_cancelled

internal fun eventSection(
    scope: LazyListScope,
    title: String,
    events: List<ShopEvents>,
    isOngoingSection: Boolean,
    horizontalContentPadding: Dp,
    onEventClick: (ShopEvent) -> Unit,
    onEventGroupClick: (ShopEvents) -> Unit,
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
                items(
                    items = events,
                    key = { eventGroup -> eventGroup.representativeEvent.venueShopId },
                ) { eventGroup ->
                    OngoingEventShopItem(
                        eventGroup = eventGroup,
                        onClick = {
                            if (eventGroup.hasMultipleEvents) {
                                onEventGroupClick(eventGroup)
                            } else {
                                onEventClick(eventGroup.representativeEvent)
                            }
                        },
                    )
                }
            }
        }
    } else {
        scope.items(
            items = events,
            key = { eventGroup -> eventGroup.representativeEvent.venueShopId },
        ) { eventGroup ->
            EventShopCard(
                eventGroup = eventGroup,
                onEventClick = onEventClick,
                modifier = Modifier.padding(horizontal = horizontalContentPadding),
            )
        }
    }
}

@Composable
private fun OngoingEventShopItem(
    eventGroup: ShopEvents,
    onClick: () -> Unit,
) {
    val event = eventGroup.representativeEvent
    Column(
        modifier =
            Modifier
                .width(72.dp)
                .noRippleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp),
        ) {
            RemoteShopImage(
                url = event.venueProfileImageUrl,
                modifier = Modifier.size(68.dp),
            )
            if (eventGroup.any(ShopEvent::isCancelledToday)) {
                AppText(
                    text = stringResource(Res.string.event_status_cancelled),
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .background(SystemColor.Warning, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = AppTextStyle.C2,
                    color = CommonColor.White,
                )
            }
            if (eventGroup.hasMultipleEvents) {
                AppText(
                    text =
                        stringResource(
                            Res.string.event_list_additional_events,
                            eventGroup.eventCount,
                        ),
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .background(InstagramColor.Blue, CircleShape)
                            .padding(4.dp),
                    style = AppTextStyle.C2,
                    color = CommonColor.White,
                )
            }
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

@Composable
internal fun EventShopCard(
    eventGroup: ShopEvents,
    onEventClick: (ShopEvent) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val shop = eventGroup.representativeEvent
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .clip(cardShape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, cardShape)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RemoteShopImage(
                url = shop.venueProfileImageUrl,
                modifier = Modifier.size(44.dp),
            )
            AppText(
                text = shop.venueShopName,
                modifier = Modifier.weight(1f),
                style = AppTextStyle.B1,
                color = GrayColor.C400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (eventGroup.any(ShopEvent::isCancelledToday)) {
                AppText(
                    text = stringResource(Res.string.event_status_cancelled),
                    modifier =
                        Modifier
                            .background(SystemColor.Warning, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = AppTextStyle.C2,
                    color = CommonColor.White,
                    maxLines = 1,
                )
            }
        }
        eventGroup.forEachIndexed { index, event ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
            EventShopRow(
                event = event,
                onClick = { onEventClick(event) },
            )
        }
    }
}

@Composable
private fun EventShopRow(
    event: ShopEvent,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().noRippleClickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(
                text = event.title,
                modifier = Modifier.weight(1f),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.isCancelledToday) {
                AppText(
                    text = stringResource(Res.string.event_status_cancelled),
                    modifier =
                        Modifier
                            .background(SystemColor.Warning, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = AppTextStyle.C2,
                    color = CommonColor.White,
                    maxLines = 1,
                )
            }
            AppText(
                text = stringResource(ShopEventResourceMapper.typeLabel(event.type)),
                modifier =
                    Modifier
                        .background(ChromaticColor.Yellow400, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                style = AppTextStyle.C2,
                color = GrayColor.C500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppText(
            text = eventDateText(event.startDate, event.endDate),
            style = AppTextStyle.B4,
            color = GrayColor.C400,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OngoingEventShopItemPreview() {
    RamapTheme {
        OngoingEventShopItem(
            eventGroup =
                ShopEvents(
                    listOf(
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
                            venueShop =
                                RamenShop(
                                    id = "preview-shop",
                                    kakaoPlaceId = null,
                                    name = "무진장이름이긴매장입니다.",
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
                                ),
                            waitingMethod = null,
                            waitingUrl = null,
                        ),
                    ),
                ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventSectionPreview(
    @PreviewParameter(EventSectionPreviewParameterProvider::class) eventGroups: List<ShopEvents>,
) {
    RamapTheme {
        var selectedEventGroup by remember { mutableStateOf<ShopEvents?>(null) }
        val title = stringResource(Res.string.event_list_ongoing_section)
        Box {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                eventSection(
                    scope = this,
                    title = title,
                    events = eventGroups,
                    isOngoingSection = true,
                    horizontalContentPadding = 15.dp,
                    onEventClick = {},
                    onEventGroupClick = { selectedEventGroup = it },
                )
            }
            selectedEventGroup?.let { eventGroup ->
                EventListBottomSheet(
                    events = eventGroup,
                    onDismiss = { selectedEventGroup = null },
                    onEventClick = { selectedEventGroup = null },
                )
            }
        }
    }
}
