package com.peto.ramap.ui.component

import androidx.compose.runtime.Composable
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
