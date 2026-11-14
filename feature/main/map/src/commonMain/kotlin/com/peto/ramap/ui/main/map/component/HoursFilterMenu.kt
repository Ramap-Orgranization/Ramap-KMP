package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.domain.model.shop.ShopVisitTimeWindow
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.component.dialog.HoursFilterDialog
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_clock
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.map_hours_filter_open_now
import ramap.shared.generated.resources.map_hours_filter_short_date
import ramap.shared.generated.resources.map_hours_filter_today
import ramap.shared.generated.resources.map_hours_filter_tomorrow
import ramap.shared.generated.resources.map_hours_filter_visit_label
import ramap.shared.generated.resources.shop_information_field_business_hours

@Composable
internal fun HoursFilterMenu(
    selected: ShopHoursFilter,
    now: LocalDateTime,
    resultCount: (ShopHoursFilter) -> Int,
    onOpen: () -> Unit,
    onApply: (ShopHoursFilter) -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val window = remember(now) { ShopVisitTimeWindow(now) }

    CategoryFilterChip(
        label = hoursFilterLabel(selected, window),
        selected = selected != ShopHoursFilter.All,
        onClick = {
            onOpen()
            isDialogOpen = true
        },
        style = AppTextStyle.B4,
        trailingIcon = Res.drawable.ic_keyboard_arrow_down,
        leadingIcon = Res.drawable.ic_clock,
    )

    if (isDialogOpen) {
        HoursFilterDialog(
            selected = selected,
            window = window,
            resultCount = resultCount,
            onDismiss = { isDialogOpen = false },
            onApply = { filter ->
                isDialogOpen = false
                onApply(filter)
            },
        )
    }
}

@Composable
private fun hoursFilterLabel(
    filter: ShopHoursFilter,
    window: ShopVisitTimeWindow,
): String =
    when (filter) {
        ShopHoursFilter.All -> stringResource(Res.string.shop_information_field_business_hours)
        ShopHoursFilter.OpenNow -> stringResource(Res.string.map_hours_filter_open_now)
        is ShopHoursFilter.VisitTime -> {
            val date = filter.dateTime.date
            val dateLabel =
                when (date) {
                    window.today -> stringResource(Res.string.map_hours_filter_today)
                    window.tomorrow -> stringResource(Res.string.map_hours_filter_tomorrow)
                    else -> stringResource(Res.string.map_hours_filter_short_date, date.month.number, date.day)
                }
            stringResource(Res.string.map_hours_filter_visit_label, dateLabel, filter.dateTime.time.toString())
        }
    }

@Preview(name = "영업시간 칩", showBackground = true)
@Composable
private fun HoursFilterMenuPreview() {
    RamapTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HoursFilterMenu(
                selected = ShopHoursFilter.VisitTime(LocalDateTime(2026, 11, 14, 19, 0)),
                now = LocalDateTime(2026, 11, 14, 18, 45),
                resultCount = { 3 },
                onOpen = {},
                onApply = {},
            )
        }
    }
}
