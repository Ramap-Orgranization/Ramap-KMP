package com.peto.ramap.ui.review.detail.component

import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_comment_delete_confirm
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun ReviewCommentDeleteDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CommonDialog(
        visible = true,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !isDeleting,
        confirmIsLoading = isDeleting,
        dismissEnabled = !isDeleting,
        dismissOnBackPress = !isDeleting,
        dismissOnClickOutside = !isDeleting,
        onDismissRequest = { if (!isDeleting) onDismiss() },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        AppText(
            text = stringResource(Res.string.review_comment_delete_confirm),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
    }
}
