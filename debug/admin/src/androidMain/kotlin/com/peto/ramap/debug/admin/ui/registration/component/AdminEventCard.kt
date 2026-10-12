package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.ui.registration.formatDateRange
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import java.time.LocalDate

@Composable
internal fun AdminEventCard(
    event: AdminManagedEvent,
    today: LocalDate,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CommonColor.White,
        border = BorderStroke(1.dp, GrayColor.C100),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(
                    text = event.shopName,
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
                if (event.isCancelledOn(today) || event.isSoldOutOn(today)) {
                    NewsBadge(
                        text = stringResource(if (event.isCancelledOn(today)) R.string.admin_event_status_badge_cancelled else R.string.admin_event_status_badge_sold_out),
                        textStyle = AppTextStyle.C2,
                        containerColor = SystemColor.Warning,
                        contentColor = CommonColor.White,
                    )
                }
            }
            AppText(
                text = event.title,
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            AppText(
                text = formatDateRange(event.startDate, if (event.eventType == "store_renewal") null else event.effectiveEndDate).orEmpty(),
                style = AppTextStyle.B4,
                color = GrayColor.C400,
            )
        }
    }
}
