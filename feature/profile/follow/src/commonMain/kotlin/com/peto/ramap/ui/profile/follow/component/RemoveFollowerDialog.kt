package com.peto.ramap.ui.profile.follow.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_remove
import ramap.shared.generated.resources.follow_remove_confirm
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun RemoveFollowerDialog(
    visible: Boolean,
    removing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !removing,
        confirmIsLoading = removing,
        dismissEnabled = !removing,
        dismissOnBackPress = !removing,
        dismissOnClickOutside = !removing,
        onDismissRequest = { if (!removing) onDismiss() },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        AppText(
            text = stringResource(Res.string.follow_remove),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = stringResource(Res.string.follow_remove_confirm),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemoveFollowerDialogPreview() {
    RamapTheme {
        RemoveFollowerDialog(
            visible = true,
            removing = false,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
