package com.peto.ramap.ui.main.event.calendar.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_legend_multiple
import ramap.shared.generated.resources.event_calendar_legend_single
import ramap.shared.generated.resources.event_calendar_notification
import ramap.shared.generated.resources.ic_notification

@Composable
internal fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendItem(
            dots = 1,
            label = stringResource(Res.string.event_calendar_legend_single),
        )
        CalendarLegendItem(
            dots = 2,
            label = stringResource(Res.string.event_calendar_legend_multiple),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_notification),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = GrayColor.C400,
            )
            AppText(
                text = stringResource(Res.string.event_calendar_notification),
                style = AppTextStyle.C2,
                color = GrayColor.C400,
            )
        }
    }
}

@Composable
private fun CalendarLegendItem(
    dots: Int,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            EventDot(color = InstagramColor.Orange)
            if (dots > 1) EventDot(color = ChromaticColor.Purple400)
        }
        AppText(text = label, style = AppTextStyle.B4, color = GrayColor.C400)
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarLegendPreview() {
    RamapTheme {
        CalendarLegend()
    }
}
