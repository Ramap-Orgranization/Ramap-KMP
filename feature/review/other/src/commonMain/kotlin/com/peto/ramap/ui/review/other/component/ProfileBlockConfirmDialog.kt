package com.peto.ramap.ui.review.other.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_block
import ramap.shared.generated.resources.review_block_confirm
import ramap.shared.generated.resources.review_profile_title
import ramap.shared.generated.resources.review_unblock
import ramap.shared.generated.resources.review_unblock_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun ProfileBlockConfirmDialog(
    visible: Boolean,
    isBlocked: Boolean,
    nickname: String,
    blocking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(if (isBlocked) Res.string.review_unblock else Res.string.review_block),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !blocking,
        confirmIsLoading = blocking,
        onDismissRequest = { if (!blocking) onDismiss() },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        Column {
            AppText(
                text = stringResource(Res.string.review_profile_title),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
        }
        AppText(
            text =
                stringResource(
                    if (isBlocked) Res.string.review_unblock_confirm else Res.string.review_block_confirm,
                    nickname,
                ),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}
