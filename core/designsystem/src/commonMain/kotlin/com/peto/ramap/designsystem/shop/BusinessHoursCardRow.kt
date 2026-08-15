package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceLine
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_break_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_closed
import ramap.shared.generated.resources.shop_detail_business_hours_closed_label_format
import ramap.shared.generated.resources.shop_detail_business_hours_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_mon
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_range_format
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_sun

@Composable
internal fun BusinessHoursCardRow(
    line: BusinessHoursResourceLine,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val mainValue = line.values.firstOrNull() ?: return
    val isClosed =
        mainValue.resource == Res.string.shop_detail_business_hours_closed ||
            mainValue.resource == Res.string.shop_detail_business_hours_closed_label_format

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = dayLabelText(line),
                modifier = Modifier.weight(1f),
                style = AppTextStyle.B1,
                color = if (isClosed) SystemColor.Warning else GrayColor.C500,
            )
            Column(horizontalAlignment = Alignment.End) {
                AppText(
                    text = mainValue.format(),
                    style = AppTextStyle.B3,
                    color = if (isClosed) SystemColor.Warning else GrayColor.C500,
                )
                line.values.drop(1).forEach { value ->
                    if (value.resource == Res.string.shop_detail_business_hours_break_time_format) {
                        AppText(
                            text = value.format(),
                            modifier =
                                Modifier
                                    .padding(top = 4.dp),
                            style = AppTextStyle.C2,
                            color = InstagramColor.Orange,
                        )
                    } else {
                        AppText(
                            text = value.format(),
                            modifier = Modifier.padding(top = 4.dp),
                            style = AppTextStyle.C2,
                            color = GrayColor.C300,
                        )
                    }
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
        }
    }
}

@Composable
private fun dayLabelText(line: BusinessHoursResourceLine): String {
    val dayLabel = stringResource(line.dayLabel)
    return line.endDayLabel?.let { endDayLabel ->
        stringResource(
            Res.string.shop_detail_business_hours_weekday_range_format,
            dayLabel,
            stringResource(endDayLabel),
        )
    } ?: dayLabel
}

@Preview(showBackground = true)
@Composable
private fun BusinessHoursCardRowPreview() {
    RamapTheme {
        Column {
            BusinessHoursCardRow(
                line =
                    BusinessHoursResourceLine(
                        dayLabel = Res.string.shop_detail_business_hours_weekday_mon,
                        values =
                            listOf(
                                UiText(
                                    Res.string.shop_detail_business_hours_time_format,
                                    listOf("11:00", "21:00"),
                                ),
                                UiText(
                                    Res.string.shop_detail_business_hours_break_time_format,
                                    listOf("15:00", "17:00"),
                                ),
                            ),
                    ),
                isLast = false,
            )
            BusinessHoursCardRow(
                line =
                    BusinessHoursResourceLine(
                        dayLabel = Res.string.shop_detail_business_hours_weekday_sun,
                        values =
                            listOf(
                                UiText(Res.string.shop_detail_business_hours_closed),
                            ),
                    ),
                isLast = true,
            )
        }
    }
}
