package com.peto.ramap.ui.main.event.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.event.calendar.model.CalendarDayEvents
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_date_format
import ramap.shared.generated.resources.event_calendar_event_count
import ramap.shared.generated.resources.event_calendar_next_month
import ramap.shared.generated.resources.event_calendar_previous_month
import ramap.shared.generated.resources.event_status_cancelled
import ramap.shared.generated.resources.ic_chevron_left
import ramap.shared.generated.resources.ic_chevron_right

@Composable
internal fun CalendarEventsBottomSheet(
    days: List<CalendarDayEvents>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onEventClick: (ShopEvent) -> Unit,
) {
    var selectedIndex by remember(days, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, days.lastIndex))
    }
    val day = days[selectedIndex]
    CommonBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        config = CommonBottomSheetConfig(isDraggable = true),
    ) { modifier ->
        Column(
            modifier =
                modifier
                    .padding(bottom = 5.dp)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalendarDateNavigator(
                date = day.date,
                onPreviousClick = { selectedIndex-- },
                onNextClick = { selectedIndex++ },
                isPreviousEnabled = selectedIndex > 0,
                isNextEnabled = selectedIndex < days.lastIndex,
            )
            AppText(
                text = stringResource(Res.string.event_calendar_event_count, day.events.size),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopEvents.groupByVenue(day.events).forEach { eventGroup ->
                    if (eventGroup.hasMultipleEvents) {
                        CalendarEventShopCard(
                            eventGroup = eventGroup,
                            date = day.date,
                            onEventClick = onEventClick,
                        )
                    } else {
                        val event = eventGroup.representativeEvent
                        EventCard(
                            event = event,
                            dateText = eventDateText(event.startDate, event.endDate),
                            isCancelled = event.isCancelledOn(day.date),
                            onClick = { onEventClick(event) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventShopCard(
    eventGroup: ShopEvents,
    date: LocalDate,
    onEventClick: (ShopEvent) -> Unit,
) {
    val shop = eventGroup.representativeEvent
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
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
        }
        eventGroup.forEachIndexed { index, event ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
            CalendarEventShopRow(
                event = event,
                date = date,
                onClick = { onEventClick(event) },
            )
        }
    }
}

@Composable
private fun CalendarEventShopRow(
    event: ShopEvent,
    date: LocalDate,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
            if (event.isCancelledOn(date)) {
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

@Composable
private fun CalendarDateNavigator(
    date: LocalDate,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    isPreviousEnabled: Boolean,
    isNextEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        CalendarDateArrow(
            painter = Res.drawable.ic_chevron_left,
            contentDescription = stringResource(Res.string.event_calendar_previous_month),
            enabled = isPreviousEnabled,
            onClick = onPreviousClick,
        )
        AppText(
            text =
                stringResource(
                    Res.string.event_calendar_date_format,
                    date.year,
                    "${date.month.number}".padStart(2, '0'),
                    "${date.day}".padStart(2, '0'),
                ),
            modifier = Modifier.width(84.dp),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )
        CalendarDateArrow(
            painter = Res.drawable.ic_chevron_right,
            contentDescription = stringResource(Res.string.event_calendar_next_month),
            enabled = isNextEnabled,
            onClick = onNextClick,
        )
    }
}

@Composable
private fun CalendarDateArrow(
    painter: DrawableResource,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Icon(
        painter = painterResource(painter),
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(36.dp)
                .padding(6.dp)
                .noRippleClickable(enabled = enabled, onClick = onClick),
        tint = if (enabled) GrayColor.C500 else GrayColor.C200,
    )
}
