package com.peto.ramap.ui.subscribed.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.notification_removal_confirm_action
import ramap.shared.generated.resources.notification_removal_confirm_title
import ramap.shared.generated.resources.notification_removal_dismiss_action

@Composable
internal fun SubscribedRemovalConfirmDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.notification_removal_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.notification_removal_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
