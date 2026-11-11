package com.peto.ramap.ui.main.my.component

import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.PublicProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_unblock
import ramap.shared.generated.resources.review_unblock_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun UnblockConfirmDialog(
    visible: Boolean,
    pendingUnblockUser: PublicProfile?,
    unblocking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return

    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_unblock),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !unblocking,
        confirmIsLoading = unblocking,
        dismissEnabled = !unblocking,
        dismissOnBackPress = !unblocking,
        dismissOnClickOutside = !unblocking,
        onDismissRequest = onDismiss,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        AppText(
            text =
                stringResource(
                    Res.string.review_unblock_confirm,
                    pendingUnblockUser?.nickname.orEmpty(),
                ),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}
