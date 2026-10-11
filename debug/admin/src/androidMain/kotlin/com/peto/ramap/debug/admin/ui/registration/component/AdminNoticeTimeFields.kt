package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminBreakTime
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
internal fun AdminNoticeTimeFields(
    type: OperatingNoticeType?,
    startTime: String?,
    endTime: String?,
    onTimesChanged: (String?, String?) -> Unit,
    onScheduleChanged: (String?, String?) -> Unit,
    breakTimes: List<AdminBreakTime>,
    onBreakTimeChanged: (Int, String, String) -> Unit,
) {
    when (type) {
        OperatingNoticeType.EARLY_CLOSING ->
            OutlinedTextField(
                value = endTime.orEmpty(),
                onValueChange = { onTimesChanged(null, it) },
                label = { AppText(text = stringResource(R.string.admin_registration_end_time), style = AppTextStyle.B4, color = GrayColor.C400) },
                modifier = Modifier.fillMaxWidth(),
            )
        OperatingNoticeType.LATE_OPENING ->
            OutlinedTextField(
                value = startTime.orEmpty(),
                onValueChange = { onTimesChanged(it.ifBlank { null }, null) },
                label = { AppText(text = stringResource(R.string.admin_notice_planned_open), style = AppTextStyle.B4, color = GrayColor.C400) },
                modifier = Modifier.fillMaxWidth(),
            )
        OperatingNoticeType.OPERATING_NOTICE -> {
            OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = startTime.orEmpty(), onValueChange = { onScheduleChanged(it, endTime) }, label = { AppText(text = stringResource(R.string.admin_notice_override_open), style = AppTextStyle.B4, color = GrayColor.C400) })
            OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = endTime.orEmpty(), onValueChange = { onScheduleChanged(startTime, it) }, label = { AppText(text = stringResource(R.string.admin_notice_override_close), style = AppTextStyle.B4, color = GrayColor.C400) })
            breakTimes.forEachIndexed { index, currentBreak ->
                OutlinedTextField(
                    value = currentBreak.start,
                    onValueChange = { onBreakTimeChanged(index, it, currentBreak.end) },
                    label = { AppText(text = stringResource(R.string.admin_notice_break_start), style = AppTextStyle.B4, color = GrayColor.C400) },
                )
                OutlinedTextField(
                    value = currentBreak.end,
                    onValueChange = { onBreakTimeChanged(index, currentBreak.start, it) },
                    label = { AppText(text = stringResource(R.string.admin_notice_break_end), style = AppTextStyle.B4, color = GrayColor.C400) },
                )
            }
        }
        else -> Unit
    }
}
