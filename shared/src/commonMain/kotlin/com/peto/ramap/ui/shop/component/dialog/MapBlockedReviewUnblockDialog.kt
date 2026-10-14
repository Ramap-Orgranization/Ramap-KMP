package com.peto.ramap.ui.shop.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.shop.review.contract.ShopReviewIntent
import com.peto.ramap.ui.shop.review.contract.ShopReviewUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.review_unblock_user_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun MapBlockedReviewUnblockDialog(
    state: ShopReviewUiState,
    onIntent: (ShopReviewIntent) -> Unit,
) {
    CommonDialog(
        visible = state.pendingUnblockReview != null,
        confirmText = stringResource(Res.string.review_unblock_user),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !state.isUnblockingReview,
        confirmIsLoading = state.isUnblockingReview,
        dismissEnabled = !state.isUnblockingReview,
        dismissOnBackPress = !state.isUnblockingReview,
        dismissOnClickOutside = !state.isUnblockingReview,
        onDismissRequest = { onIntent(ShopReviewIntent.OnBlockedReviewUnblockDismissed) },
        onDismiss = { onIntent(ShopReviewIntent.OnBlockedReviewUnblockDismissed) },
        onConfirm = { onIntent(ShopReviewIntent.OnBlockedReviewUnblockConfirmed) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppText(
                text = stringResource(Res.string.review_unblock_user),
                style = AppTextStyle.B2,
                color = GrayColor.C500,
            )
            AppText(
                text = stringResource(Res.string.review_unblock_user_confirm),
                style = AppTextStyle.B2,
                color = GrayColor.C500,
            )
        }
    }
}
