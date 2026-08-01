package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.component.eventDateText

internal fun eventSection(
    scope: LazyListScope,
    title: String,
    events: List<ShopEvent>,
    isHorizontal: Boolean,
    horizontalContentPadding: Dp = 0.dp,
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
    if (isHorizontal) {
        scope.item(key = "$title-events") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalContentPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(events, key = ShopEvent::id) { event ->
                    EventCard(
                        event = event,
                        dateText = eventDateText(event.startDate, event.endDate),
                        onClick = { onEventClick(event) },
                        modifier = Modifier.width(280.dp),
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
