package com.peto.ramap.ui.main.map.component.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_hours_filter_sun
import ramap.shared.generated.resources.ic_hours_filter_sunrise
import ramap.shared.generated.resources.map_hours_filter_am
import ramap.shared.generated.resources.map_hours_filter_date_description
import ramap.shared.generated.resources.map_hours_filter_month
import ramap.shared.generated.resources.map_hours_filter_pm
import ramap.shared.generated.resources.map_hours_filter_today

private const val HOURS_PER_ROW = 4
private const val NOON_HOUR = 12

@Composable
internal fun HoursFilterVisitPicker(
    visit: LocalDateTime,
    window: ShopVisitTimeWindow,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VisitDateStrip(
            date = visit.date,
            window = window,
            onDateSelected = onDateSelected,
        )
        HorizontalDivider(color = GrayColor.C100)

        val (morning, afternoon) = window.availableTimes(visit.date).partition { it.hour < NOON_HOUR }
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VisitTimeGrid(
                icon = Res.drawable.ic_hours_filter_sunrise,
                title = stringResource(Res.string.map_hours_filter_am),
                times = morning,
                selectedTime = visit.time,
                onTimeSelected = onTimeSelected,
            )
            VisitTimeGrid(
                icon = Res.drawable.ic_hours_filter_sun,
                title = stringResource(Res.string.map_hours_filter_pm),
                times = afternoon,
                selectedTime = visit.time,
                onTimeSelected = onTimeSelected,
            )
        }
    }
}

@Composable
private fun VisitDateStrip(
    date: LocalDate,
    window: ShopVisitTimeWindow,
    onDateSelected: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = stringResource(Res.string.map_hours_filter_month, date.year, date.month.number),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            window.availableDates.forEach { availableDate ->
                VisitDateTile(
                    date = availableDate,
                    isToday = availableDate == window.today,
                    isSelected = availableDate == date,
                    onClick = { onDateSelected(availableDate) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VisitDateTile(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekday = stringResource(BusinessHoursWeekday.entries[date.dayOfWeek.ordinal].resource)
    val description = stringResource(Res.string.map_hours_filter_date_description, date.month.number, date.day, weekday)
    val weekdayColor = if (isSelected) BrandColor.OnFill else GrayColor.C300
    val dateColor = if (isSelected) BrandColor.OnFill else GrayColor.C500
    val todayColor = if (isSelected) BrandColor.OnFill else BrandColor.SecondPrimary

    Column(
        modifier =
            modifier
                .heightIn(min = 66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) BrandColor.SubtleFill else CommonColor.White)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppText(
            text = weekday,
            style = AppTextStyle.C1,
            color = weekdayColor,
        )
        AppText(
            text = date.day.toString(),
            style = AppTextStyle.T2,
            color = dateColor,
        )
        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (isToday) {
                AppText(
                    text = stringResource(Res.string.map_hours_filter_today),
                    style = AppTextStyle.C2,
                    color = todayColor,
                )
            }
        }
    }
}

@Composable
private fun VisitTimeGrid(
    icon: DrawableResource,
    title: String,
    times: List<LocalTime>,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    if (times.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            AppText(
                text = title,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                modifier = Modifier.semantics { heading() },
            )
        }
        times.chunked(HOURS_PER_ROW).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { time ->
                    val isSelected = time == selectedTime
                    AppButton(
                        text = time.toString(),
                        onClick = { onTimeSelected(time) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics {
                                    selected = isSelected
                                    role = Role.RadioButton
                                },
                        textColor = if (isSelected) BrandColor.OnFill else GrayColor.C400,
                        textStyle = if (isSelected) AppTextStyle.B1 else AppTextStyle.B2,
                        backgroundColor = if (isSelected) BrandColor.Primary else CommonColor.White,
                        border = if (isSelected) null else BorderStroke(1.dp, GrayColor.C100),
                        cornerRadius = 8.dp,
                        height = 44.dp,
                    )
                }
                repeat(HOURS_PER_ROW - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(name = "방문 시간 선택기", showBackground = true, widthDp = 360)
@Composable
private fun HoursFilterVisitPickerPreview() {
    RamapTheme {
        HoursFilterVisitPicker(
            visit = LocalDateTime(2026, 11, 15, 19, 0),
            window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 14, 18, 45)),
            onDateSelected = {},
            onTimeSelected = {},
        )
    }
}
