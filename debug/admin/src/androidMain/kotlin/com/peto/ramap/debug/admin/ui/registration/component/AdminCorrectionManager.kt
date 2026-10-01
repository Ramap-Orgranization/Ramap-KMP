package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminCorrectionPreview
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
internal fun AdminCorrectionManager(
    request: String,
    preview: AdminCorrectionPreview?,
    isLoading: Boolean,
    onRequestChanged: (String) -> Unit,
    onPreviewRequested: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppText(stringResource(R.string.admin_correction_title), AppTextStyle.B1, GrayColor.C500)
        OutlinedTextField(
            value = request,
            onValueChange = onRequestChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { AppText(stringResource(R.string.admin_correction_request), AppTextStyle.B4, GrayColor.C400) },
        )
        AppButton(
            text = stringResource(R.string.admin_correction_preview),
            onClick = onPreviewRequested,
            enabled = request.isNotBlank() && !isLoading,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CommonColor.Black,
        )
    }
    if (preview != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { AppText(stringResource(R.string.admin_correction_confirm_title), AppTextStyle.T2, GrayColor.C500) },
            text = { AppText(preview.summary, AppTextStyle.B3, GrayColor.C400) },
            confirmButton = { TextButton(onClick = onConfirm) { AppText(stringResource(R.string.admin_correction_confirm), AppTextStyle.B2, GrayColor.C500) } },
            dismissButton = { TextButton(onClick = onDismiss) { AppText(stringResource(R.string.admin_registration_cancel), AppTextStyle.B2, GrayColor.C400) } },
        )
    }
}
