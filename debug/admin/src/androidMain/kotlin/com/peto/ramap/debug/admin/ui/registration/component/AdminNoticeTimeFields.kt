package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminBreakTime
import com.peto.ramap.domain.model.notice.OperatingNoticeType

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
                label = { androidx.compose.material3.Text(stringResource(R.string.admin_registration_end_time)) },
                modifier = Modifier,
            )
        OperatingNoticeType.LATE_OPENING ->
            OutlinedTextField(
                value = startTime.orEmpty(),
                onValueChange = { onTimesChanged(it.ifBlank { null }, null) },
                label = { androidx.compose.material3.Text(stringResource(R.string.admin_notice_planned_open)) },
                modifier = Modifier,
            )
        OperatingNoticeType.OPERATING_NOTICE -> {
            OutlinedTextField(value = startTime.orEmpty(), onValueChange = { onScheduleChanged(it, endTime) }, label = { androidx.compose.material3.Text(stringResource(R.string.admin_notice_override_open)) })
            OutlinedTextField(value = endTime.orEmpty(), onValueChange = { onScheduleChanged(startTime, it) }, label = { androidx.compose.material3.Text(stringResource(R.string.admin_notice_override_close)) })
            breakTimes.forEachIndexed { index, currentBreak ->
                OutlinedTextField(
                    value = currentBreak.start,
                    onValueChange = { onBreakTimeChanged(index, it, currentBreak.end) },
                    label = { androidx.compose.material3.Text(stringResource(R.string.admin_notice_break_start)) },
                )
                OutlinedTextField(
                    value = currentBreak.end,
                    onValueChange = { onBreakTimeChanged(index, currentBreak.start, it) },
                    label = { androidx.compose.material3.Text(stringResource(R.string.admin_notice_break_end)) },
                )
            }
        }
        else -> Unit
    }
}
