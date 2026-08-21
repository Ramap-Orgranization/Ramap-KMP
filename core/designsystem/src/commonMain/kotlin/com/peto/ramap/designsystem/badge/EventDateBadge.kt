package com.peto.ramap.designsystem.badge

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_event_calendar

@Composable
fun EventDateBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = CommonColor.Black,
                    shape = RoundedCornerShape(4.dp),
                ).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_event_calendar),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = GrayColor.C400,
        )
        AppText(
            text = text,
            style = AppTextStyle.B4,
            color = GrayColor.C400,
        )
    }
}

@Preview
@Composable
private fun EventDateBadgePreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventDateBadge(text = eventDateText("2026-08-21", "2026-08-21"))
            EventDateBadge(text = eventDateText("2026-08-21", "2026-08-22"))
            EventDateBadge(text = eventDateText("2026-08-21", "2026-09-01"))
        }
    }
}
