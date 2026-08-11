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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    events: List<ShopEvent>,
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
        if (events.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EventDot(color = InstagramColor.Orange)
                if (events.size > 1) EventDot(color = InstagramColor.Pink)
            }
        }
    }
}
