package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.review_unblock_user_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun MapBlockedReviewUnblockDialog(
    state: MapUiState,
    onIntent: (MapIntent) -> Unit,
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
        onDismissRequest = { onIntent(MapIntent.OnBlockedReviewUnblockDismissed) },
        onDismiss = { onIntent(MapIntent.OnBlockedReviewUnblockDismissed) },
        onConfirm = { onIntent(MapIntent.OnBlockedReviewUnblockConfirmed) },
    ) {
        AppText(
            text = stringResource(Res.string.review_unblock_user_confirm),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}
