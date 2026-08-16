package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.EventShopGroupCard
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
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
                    .padding(start = 15.dp),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
    }
    if (isOngoingSection) {
        scope.item(key = "$title-events") {
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
            EventShopGroupCard(
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
            modifier = Modifier.fillMaxWidth().height(36.dp),
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
private fun OngoingEventShopItemPreview(
    @PreviewParameter(EventSectionPreviewParameterProvider::class) eventGroups: List<ShopEvents>,
) {
    RamapTheme {
        OngoingEventShopItem(
            eventGroup = eventGroups.first(),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
