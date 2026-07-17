package com.peto.ramap.designsystem.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_action
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_dismiss
import ramap.shared.generated.resources.login_required_message

@Composable
fun LoginGuideDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.login_required_action),
        dismissText = stringResource(Res.string.login_required_dismiss),
        onDismissRequest = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.login_required_message),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.login_required_description),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
