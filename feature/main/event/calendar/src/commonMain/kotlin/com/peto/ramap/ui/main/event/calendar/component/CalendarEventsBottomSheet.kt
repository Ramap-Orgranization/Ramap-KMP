package com.peto.ramap.ui.main.event.calendar.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.card.EventShopGroupCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
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
                        EventShopGroupCard(
                            eventGroup = eventGroup,
                            onEventClick = onEventClick,
                            isCancelled = { event -> event.isCancelledOn(day.date) },
                            isSoldOut = { event -> event.isSoldOutOn(day.date) },
                            showHeaderCancelledBadge = false,
                        )
                    } else {
                        val event = eventGroup.representativeEvent
                        EventCard(
                            event = event,
                            dateText =
                                eventDateText(
                                    event.startDate,
                                    if (event.type == ShopEventType.STORE_RENEWAL) {
                                        event.startDate
                                    } else {
                                        event.endDate
                                    },
                                ),
                            isCancelled = event.isCancelledOn(day.date),
                            isSoldOut = event.isSoldOutOn(day.date),
                            onClick = { onEventClick(event) },
                        )
                    }
                }
            }
        }
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
