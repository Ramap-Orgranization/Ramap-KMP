package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
internal fun AdminNoticeTimeFields(
    type: OperatingNoticeType?,
    endTime: String?,
    isError: Boolean,
    onTimesChanged: (String?, String?) -> Unit,
) {
    if (type != OperatingNoticeType.EARLY_CLOSING) return
    OutlinedTextField(
        value = endTime.orEmpty(),
        isError = isError,
        onValueChange = { onTimesChanged(null, it.ifBlank { null }) },
        label = { AppText(text = stringResource(R.string.admin_registration_end_time), style = AppTextStyle.B4, color = GrayColor.C400) },
        modifier = Modifier.fillMaxWidth(),
    )
}
