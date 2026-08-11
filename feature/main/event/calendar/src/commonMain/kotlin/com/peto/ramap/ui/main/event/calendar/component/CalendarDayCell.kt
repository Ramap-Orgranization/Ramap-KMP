package com.peto.ramap.ui.main.event.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_notification
import ramap.shared.generated.resources.ic_notification
import kotlin.time.Clock

@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    events: List<ShopEvent>,
    hasNotification: Boolean,
    onSingleEventClick: (ShopEvent) -> Unit,
    onMultipleEventsClick: (LocalDate, List<ShopEvent>) -> Unit,
) {
    val isToday = date == Clock.System.todayIn(TimeZone.currentSystemDefault())
    val clickModifier =
        when {
            events.size == 1 -> Modifier.noRippleClickable { onSingleEventClick(events.single()) }
            events.size >= 2 -> Modifier.noRippleClickable { onMultipleEventsClick(date, events) }
            else -> Modifier
        }
    Column(
        modifier = clickModifier.height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isToday) CommonColor.Black else CommonColor.White),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = date.day.toString(),
                style = AppTextStyle.B3,
                color = if (isToday) CommonColor.White else GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        }
        if (events.isNotEmpty() || hasNotification) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasNotification) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_notification),
                        contentDescription = stringResource(Res.string.event_calendar_notification),
                        modifier = Modifier.size(10.dp),
                        tint = ChromaticColor.Pink400,
                    )
                }
                if (!hasNotification && events.isNotEmpty()) {
                    EventDot(color = InstagramColor.Orange)
                    if (events.size > 1) EventDot(color = InstagramColor.Pink)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellNotificationPreview() {
    RamapTheme {
        CalendarDayCell(
            date = LocalDate(2025, 1, 1),
            events = emptyList(),
            hasNotification = true,
            onSingleEventClick = {},
            onMultipleEventsClick = { _, _ -> },
        )
    }
}
