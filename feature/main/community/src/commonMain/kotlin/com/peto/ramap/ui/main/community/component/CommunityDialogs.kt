package com.peto.ramap.ui.main.community.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.dialog.ReviewReportDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.community.contract.CommunityIntent
import com.peto.ramap.ui.main.community.contract.CommunityUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.review_unblock_user_confirm
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun CommunityDialogs(
    state: CommunityUiState,
    onIntent: (CommunityIntent) -> Unit,
) {
    var reason by remember(state.reportReview?.id) { mutableStateOf<ReportReason?>(null) }
    var details by remember(state.reportReview?.id) { mutableStateOf("") }
    ReviewReportDialog(
        visible = state.reportReview != null,
        selectedReason = reason,
        onReasonSelected = {
            reason = it
            details = details.take(it.maxDetailsLength)
        },
        details = details,
        onDetailsChanged = { details = it },
        isActing = state.reporting,
        onDismissRequest = { onIntent(CommunityIntent.DismissReport) },
        onSubmit = { selected, text -> onIntent(CommunityIntent.SubmitReport(selected, text)) },
    )
    CommonDialog(
        visible = state.unblockReview != null,
        confirmText = stringResource(Res.string.review_unblock_user),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !state.unblocking,
        confirmIsLoading = state.unblocking,
        dismissEnabled = !state.unblocking,
        dismissOnBackPress = !state.unblocking,
        dismissOnClickOutside = !state.unblocking,
        onDismissRequest = { onIntent(CommunityIntent.DismissUnblock) },
        onDismiss = { onIntent(CommunityIntent.DismissUnblock) },
        onConfirm = { onIntent(CommunityIntent.ConfirmUnblock) },
    ) {
        AppText(
            text = stringResource(Res.string.review_unblock_user),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(Res.string.review_unblock_user_confirm),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
    val deleting = state.deleteReview?.id != null && state.deleteReview.id == state.actingId
    CommonDialog(
        visible = state.deleteReview != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !deleting,
        confirmIsLoading = deleting,
        dismissEnabled = !deleting,
        dismissOnBackPress = !deleting,
        dismissOnClickOutside = !deleting,
        onDismissRequest = { onIntent(CommunityIntent.DismissDelete) },
        onDismiss = { onIntent(CommunityIntent.DismissDelete) },
        onConfirm = { onIntent(CommunityIntent.ConfirmDelete) },
    ) {
        AppText(
            text = stringResource(Res.string.review_delete_confirm_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(Res.string.review_delete_confirm),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}
