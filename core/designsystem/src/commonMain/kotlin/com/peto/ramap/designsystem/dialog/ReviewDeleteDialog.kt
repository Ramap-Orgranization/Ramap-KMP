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
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun ReviewDeleteDialog(
    visible: Boolean,
    isDeleting: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !isDeleting,
        confirmIsLoading = isDeleting,
        dismissEnabled = !isDeleting,
        dismissOnBackPress = !isDeleting,
        dismissOnClickOutside = !isDeleting,
        onDismissRequest = { if (!isDeleting) onDismissRequest() },
        onDismiss = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
    ) {
        AppText(
            text = stringResource(Res.string.review_delete_confirm_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = stringResource(Res.string.review_delete_confirm),
            modifier = Modifier.padding(top = 8.dp),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
    }
}
