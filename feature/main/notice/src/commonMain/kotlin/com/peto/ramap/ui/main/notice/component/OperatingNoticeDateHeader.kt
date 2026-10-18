package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursWeekday
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_scheduled_date_header

@Composable
internal fun OperatingNoticeDateHeader(date: LocalDate) {
    val weekday = BusinessHoursWeekday.valueOf(date.dayOfWeek.name.take(3))
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GrayColor.C050)
                .heightIn(min = 40.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        AppText(
            text =
                stringResource(
                    Res.string.operating_notice_scheduled_date_header,
                    date.month.number
                        .toString()
                        .padStart(2, '0'),
                    date.day.toString().padStart(2, '0'),
                    stringResource(weekday.resource),
                ),
            style = AppTextStyle.B3,
            color = GrayColor.C400,
        )
    }
}

@Preview(name = "예정 날짜", showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeDateHeaderPreview() {
    RamapTheme {
        OperatingNoticeDateHeader(date = LocalDate(2026, 10, 21))
    }
}
