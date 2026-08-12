package com.peto.ramap.designsystem.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_date_end_unknown
import ramap.shared.generated.resources.event_date_range_format
import ramap.shared.generated.resources.event_single_date_format

@Composable
fun eventDateText(
    startDate: String,
    endDate: String?,
): String {
    val start = parseDate(startDate) ?: return startDate
    if (endDate == null) {
        return stringResource(Res.string.event_date_end_unknown, start.first, start.second, start.third)
    }
    if (startDate == endDate) {
        return stringResource(Res.string.event_single_date_format, start.first, start.second, start.third)
    }
    val end = parseDate(endDate) ?: return endDate
    return stringResource(
        Res.string.event_date_range_format,
        start.first,
        start.second,
        start.third,
        end.first,
        end.second,
        end.third,
    )
}

private fun parseDate(date: String): Triple<String, Int, Int>? {
    if (
        date.length != 10 ||
        date[4] != '-' ||
        date[7] != '-' ||
        !date.indices.all { it == 4 || it == 7 || date[it] in '0'..'9' }
    ) {
        return null
    }
    return Triple(date.take(4), date.substring(5, 7).toInt(), date.takeLast(2).toInt())
}

@Preview
@Composable
internal fun EventDateTextPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val singleDate = eventDateText("2026-07-28", "2026-07-28")
            AppText(
                text = "Single date: $singleDate",
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            val rangeDate = eventDateText("2026-07-28", "2026-08-15")
            AppText(
                text = "Range date: $rangeDate",
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            val unknownEnd = eventDateText("2026-07-28", null)
            AppText(
                text = "Unknown end: $unknownEnd",
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            val invalidDate = eventDateText("invalid", null)
            AppText(
                text = "Invalid date: $invalidDate",
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
        }
    }
}
