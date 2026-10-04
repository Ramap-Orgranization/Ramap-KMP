package com.peto.ramap.designsystem.notice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.notice.OperatingNoticeDay
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor

@Composable
fun OperatingNoticeSchedule(
    days: List<OperatingNoticeDay>,
    businessHours: BusinessHours? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        days.sortedBy { it.date }.forEach { day ->
            val labels = ShopOperatingNoticeResourceMapper.daySchedule(day, businessHours).map { it.format() }
            AppText(
                text = (listOf(day.date.toString()) + labels).joinToString(" · "),
                style = AppTextStyle.B2,
                color = if (day.closed) SystemColor.Warning else GrayColor.C500,
            )
        }
    }
}
