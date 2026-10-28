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
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.review_unblock_user_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun ReviewUnblockDialog(
    visible: Boolean,
    isUnblocking: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_unblock_user),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !isUnblocking,
        confirmIsLoading = isUnblocking,
        dismissEnabled = !isUnblocking,
        dismissOnBackPress = !isUnblocking,
        dismissOnClickOutside = !isUnblocking,
        onDismissRequest = { if (!isUnblocking) onDismissRequest() },
        onDismiss = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
    ) {
        AppText(
            text = stringResource(Res.string.review_unblock_user),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = stringResource(Res.string.review_unblock_user_confirm),
            modifier = Modifier.padding(top = 8.dp),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
    }
}
