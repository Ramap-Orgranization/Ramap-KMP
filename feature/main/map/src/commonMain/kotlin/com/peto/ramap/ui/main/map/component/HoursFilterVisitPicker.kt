package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursWeekday
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.ShopVisitTimeWindow
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_hours_filter_am
import ramap.shared.generated.resources.map_hours_filter_date_description
import ramap.shared.generated.resources.map_hours_filter_month
import ramap.shared.generated.resources.map_hours_filter_pm
import ramap.shared.generated.resources.map_hours_filter_today

@Composable
internal fun HoursFilterVisitPicker(
    visit: LocalDateTime,
    window: ShopVisitTimeWindow,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = GrayColor.C100)

        AppText(
            text = stringResource(Res.string.map_hours_filter_month, visit.year, visit.month.number),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val dates = window.availableDates
            val gap = 4.dp
            val tileWidth = ((maxWidth - gap * dates.lastIndex) / dates.size).coerceIn(32.dp, 48.dp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                dates.forEach { date ->
                    VisitDateTile(
                        date = date,
                        isToday = date == window.today,
                        isSelected = date == visit.date,
                        width = tileWidth,
                        onClick = { onDateSelected(date) },
                    )
                }
            }
        }
        HorizontalDivider(color = GrayColor.C100)
        val (morning, afternoon) = window.availableTimes(visit.date).partition { it.hour < 12 }
        VisitTimeGrid(
            title = stringResource(Res.string.map_hours_filter_am),
            times = morning,
            selectedTime = visit.time,
            onTimeSelected = onTimeSelected,
        )
        VisitTimeGrid(
            title = stringResource(Res.string.map_hours_filter_pm),
            times = afternoon,
            selectedTime = visit.time,
            onTimeSelected = onTimeSelected,
        )
    }
}

@Composable
private fun VisitDateTile(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    width: Dp,
    onClick: () -> Unit,
) {
    val weekday = stringResource(BusinessHoursWeekday.entries[date.dayOfWeek.ordinal].resource)
    val description = stringResource(Res.string.map_hours_filter_date_description, date.month.number, date.day, weekday)
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .width(width)
                .heightIn(min = 64.dp)
                .semantics {
                    contentDescription = description
                    selected = isSelected
                },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BrandColor.Primary else CommonColor.White,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppText(
                text = weekday,
                style = AppTextStyle.C1,
                color = if (isSelected) CommonColor.White else GrayColor.C500,
            )
            AppText(
                text = date.day.toString(),
                style = AppTextStyle.T2,
                color = if (isSelected) CommonColor.White else GrayColor.C500,
            )
            Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                if (isToday) {
                    AppText(
                        text = stringResource(Res.string.map_hours_filter_today),
                        style = AppTextStyle.C1,
                        color = if (isSelected) CommonColor.White else GrayColor.C500,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitTimeGrid(
    title: String,
    times: List<LocalTime>,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    if (times.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppText(text = title, style = AppTextStyle.C1, color = GrayColor.C400)
        times.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { time ->
                    val isSelected = time == selectedTime
                    Surface(
                        onClick = { onTimeSelected(time) },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { selected = isSelected },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandColor.Primary else GrayColor.C100,
                        border = if (isSelected) BorderStroke(1.dp, BrandColor.Primary) else null,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppText(
                                text = time.toString(),
                                style = AppTextStyle.B2,
                                color = if (isSelected) CommonColor.White else GrayColor.C500,
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Preview(name = "방문 시간 선택기", showBackground = true, widthDp = 360)
@Composable
private fun HoursFilterVisitPickerPreview() {
    RamapTheme {
        HoursFilterVisitPicker(
            visit = LocalDateTime(2026, 11, 14, 19, 0),
            window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 14, 18, 45)),
            onDateSelected = {},
            onTimeSelected = {},
        )
    }
}
