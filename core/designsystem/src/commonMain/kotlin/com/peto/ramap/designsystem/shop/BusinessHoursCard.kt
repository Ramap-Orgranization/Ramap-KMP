package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.BusinessHoursDay
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_weekly_title

@Composable
internal fun BusinessHoursCard(
    businessHours: BusinessHours,
    modifier: Modifier = Modifier,
) {
    val lines = BusinessHoursResourceMapper.all(businessHours)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(Res.string.shop_detail_business_hours_weekly_title),
                style = AppTextStyle.B1,
                color = GrayColor.C300,
            )
            BusinessHoursNotice()
        }
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            lines.forEachIndexed { index, line ->
                BusinessHoursCardRow(
                    line = line,
                    isLast = index == lines.lastIndex,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BusinessHoursCardPreview() {
    RamapTheme {
        BusinessHoursCard(
            businessHours =
                BusinessHours(
                    weekly =
                        mapOf(
                            "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                            "sun" to BusinessHoursDay(true, null, null, false, null),
                        ),
                    breakTimes = emptyMap(),
                    lastOrders = emptyMap(),
                    notice = "재료 소진 시 조기 마감될 수 있습니다.",
                ),
        )
    }
}
