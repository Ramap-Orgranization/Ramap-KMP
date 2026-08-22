package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.EventShopGroupCard
import com.peto.ramap.designsystem.component.ShopThumbnail
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
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
import ramap.shared.generated.resources.event_unread_badge

internal fun eventSection(
    scope: LazyListScope,
    title: String,
    events: List<ShopEvents>,
    isOngoingSection: Boolean,
    useHorizontalScroll: Boolean = true,
    horizontalContentPadding: Dp,
    unreadEventIds: Set<String>,
    onEventDisplayed: (String) -> Unit,
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
                    .padding(start = 15.dp, bottom = 5.dp),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
    }
    if (isOngoingSection) {
        scope.item(key = "$title-events") {
            if (useHorizontalScroll) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = horizontalContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = events,
                        key = { eventGroup -> eventGroup.representativeEvent.venueShopId },
                    ) { eventGroup ->
                        OngoingEventShopItem(
                            eventGroup = eventGroup,
                            unreadEventIds = unreadEventIds,
                            onEventDisplayed = onEventDisplayed,
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
            } else {
                FlowRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalContentPadding),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            space = 10.dp,
                            alignment = Alignment.CenterHorizontally,
                        ),
                ) {
                    events.forEach { eventGroup ->
                        OngoingEventShopItem(
                            eventGroup = eventGroup,
                            unreadEventIds = unreadEventIds,
                            onEventDisplayed = onEventDisplayed,
                            onClick = {
                                if (eventGroup.hasMultipleEvents) {
                                    onEventGroupClick(eventGroup)
                                } else {
                                    onEventClick(eventGroup.representativeEvent)
                                }
                            },
                        )
                    }
                    repeat(10) {
                        Spacer(modifier = Modifier.width(72.dp).height(0.dp))
                    }
                }
            }
        }
    } else {
        scope.items(
            items = events,
            key = { eventGroup -> eventGroup.representativeEvent.venueShopId },
        ) { eventGroup ->
            EventShopGroupCard(
                eventGroup = eventGroup,
                onEventClick = onEventClick,
                modifier = Modifier.padding(horizontal = horizontalContentPadding),
                unreadEventIds = unreadEventIds,
                onEventDisplayed = onEventDisplayed,
            )
        }
    }
}

@Composable
private fun OngoingEventShopItem(
    eventGroup: ShopEvents,
    unreadEventIds: Set<String>,
    onEventDisplayed: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val event = eventGroup.representativeEvent
    if (event.id in unreadEventIds) {
        LaunchedEffect(event.id) {
            onEventDisplayed(event.id)
        }
    }
    ShopThumbnail(
        imageUrl = event.venueProfileImageUrl,
        name = event.venueShopName,
        modifier = modifier,
        onClick = onClick,
        badge = {
            ShopEventResourceMapper
                .statusLabel(
                    isCancelled = eventGroup.any(ShopEvent::isCancelledToday),
                    isSoldOut = eventGroup.any(ShopEvent::isSoldOutToday),
                )?.let { statusLabel ->
                    AppText(
                        text = stringResource(statusLabel),
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .background(SystemColor.Warning, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = AppTextStyle.C2,
                        color = CommonColor.White,
                    )
                }
        },
        topEndBadge = {
            if (eventGroup.any { it.id in unreadEventIds }) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .size(18.dp)
                            .background(
                                InstagramColor.Yellow,
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = stringResource(Res.string.event_unread_badge),
                        style = AppTextStyle.C2,
                        color = GrayColor.C500,
                    )
                }
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
                            .background(ChromaticColor.Blue400, CircleShape)
                            .padding(4.dp),
                    style = AppTextStyle.C2,
                    color = CommonColor.White,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun OngoingEventShopItemPreview(
    @PreviewParameter(EventSectionPreviewParameterProvider::class) eventGroups: List<ShopEvents>,
) {
    RamapTheme {
        OngoingEventShopItem(
            eventGroup = eventGroups.first(),
            unreadEventIds = setOf(eventGroups.first().representativeEvent.id),
            onEventDisplayed = {},
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
        val unreadEventIds = setOf(eventGroups.first().representativeEvent.id)
        Box {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                eventSection(
                    scope = this,
                    title = title,
                    events = eventGroups,
                    isOngoingSection = true,
                    horizontalContentPadding = 15.dp,
                    unreadEventIds = unreadEventIds,
                    onEventDisplayed = {},
                    onEventClick = {},
                    onEventGroupClick = { selectedEventGroup = it },
                )
            }
            selectedEventGroup?.let { eventGroup ->
                EventListBottomSheet(
                    events = eventGroup,
                    unreadEventIds = unreadEventIds,
                    onEventDisplayed = {},
                    onDismiss = { selectedEventGroup = null },
                    onEventClick = { selectedEventGroup = null },
                )
            }
        }
    }
}
