package com.peto.ramap.ui.shop.review.component.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun ShopReviewDeleteDialog(
    review: Review?,
    actingReviewId: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CommonDialog(
        visible = review != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = review?.id != actingReviewId,
        confirmIsLoading = review?.id == actingReviewId,
        dismissOnBackPress = review?.id != actingReviewId,
        dismissOnClickOutside = review?.id != actingReviewId,
        onDismissRequest = { if (review?.id != actingReviewId) onDismiss() },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
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
