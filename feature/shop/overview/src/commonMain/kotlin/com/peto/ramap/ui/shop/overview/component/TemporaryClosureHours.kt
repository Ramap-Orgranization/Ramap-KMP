package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceLine
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceMapper
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursWeekday
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.business_hours_today_closed
import ramap.shared.generated.resources.ic_calendar
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.shop_detail_business_hours_collapse
import ramap.shared.generated.resources.shop_detail_business_hours_expand
import ramap.shared.generated.resources.shop_detail_regular_business_hours

@Composable
internal fun TemporaryClosureHours(
    closure: BusinessHoursStatus.TemporaryClosure,
    lines: List<BusinessHoursResourceLine>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = ChromaticColor.Red050,
                        shape = RoundedCornerShape(10.dp),
                    ).border(
                        width = 1.dp,
                        color = SystemColor.Warning.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = SystemColor.Warning,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                AppText(
                    text = stringResource(Res.string.business_hours_today_closed),
                    style = AppTextStyle.T3,
                    color = SystemColor.Warning,
                )
                AppText(
                    text =
                        BusinessHoursStatusResourceMapper
                            .closurePeriod(
                                status = closure,
                                startWeekday = stringResource(BusinessHoursWeekday.entries[closure.startDate.dayOfWeek.ordinal].resource),
                                endWeekday = stringResource(BusinessHoursWeekday.entries[(closure.endDate ?: closure.startDate).dayOfWeek.ordinal].resource),
                            ).format(),
                    style = AppTextStyle.B2,
                    color = GrayColor.C500,
                )
            }
        }
        if (lines.isNotEmpty()) {
            BusinessHoursNotice()
            SectionCard {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .noRippleClickable(onClick = onToggle)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = stringResource(Res.string.shop_detail_regular_business_hours),
                        modifier = Modifier.weight(1f),
                        style = AppTextStyle.B1,
                        color = GrayColor.C500,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                        contentDescription =
                            stringResource(
                                if (isExpanded) Res.string.shop_detail_business_hours_collapse else Res.string.shop_detail_business_hours_expand,
                            ),
                        modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
                        tint = GrayColor.C400,
                    )
                }
                if (isExpanded) {
                    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                        SectionCard {
                            lines.forEachIndexed { index, line ->
                                BusinessHoursCardRow(
                                    line = line,
                                    isLast = index == lines.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemporaryClosureHoursPreview() {
    val closure =
        BusinessHoursStatus.TemporaryClosure(
            startDate = LocalDate(2026, 10, 14),
            endDate = LocalDate(2026, 10, 15),
        )
    val businessHours =
        BusinessHours(
            weekly =
                mapOf(
                    "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                    "tue" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                ),
            breakTimes = emptyMap(),
            lastOrders = emptyMap(),
            notice = null,
        )
    val lines = BusinessHoursResourceMapper.all(businessHours)
    RamapTheme {
        TemporaryClosureHours(
            closure = closure,
            lines = lines,
            isExpanded = false,
            onToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemporaryClosureHoursExpandedPreview() {
    val closure =
        BusinessHoursStatus.TemporaryClosure(
            startDate = LocalDate(2026, 10, 14),
            endDate = null,
        )
    val businessHours =
        BusinessHours(
            weekly =
                mapOf(
                    "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                ),
            breakTimes = emptyMap(),
            lastOrders = emptyMap(),
            notice = null,
        )
    val lines = BusinessHoursResourceMapper.all(businessHours)
    RamapTheme {
        TemporaryClosureHours(
            closure = closure,
            lines = lines,
            isExpanded = true,
            onToggle = {},
        )
    }
}
