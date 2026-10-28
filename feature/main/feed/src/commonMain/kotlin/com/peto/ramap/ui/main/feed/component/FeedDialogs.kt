package com.peto.ramap.ui.main.feed.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.dialog.ReviewDeleteDialog
import com.peto.ramap.designsystem.dialog.ReviewReportDialog
import com.peto.ramap.designsystem.dialog.ReviewUnblockDialog
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import com.peto.ramap.ui.main.feed.contract.FeedUiState

@Composable
internal fun FeedDialogs(
    state: FeedUiState,
    onIntent: (FeedIntent) -> Unit,
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
        onDismissRequest = { onIntent(FeedIntent.DismissReport) },
        onSubmit = { selected, text -> onIntent(FeedIntent.SubmitReport(selected, text)) },
    )
    ReviewUnblockDialog(
        visible = state.unblockReview != null,
        isUnblocking = state.unblocking,
        onDismissRequest = { onIntent(FeedIntent.DismissUnblock) },
        onConfirm = { onIntent(FeedIntent.ConfirmUnblock) },
    )
    val deleting = state.deleteReview?.id != null && state.deleteReview.id == state.actingId
    ReviewDeleteDialog(
        visible = state.deleteReview != null,
        isDeleting = deleting,
        onDismissRequest = { onIntent(FeedIntent.DismissDelete) },
        onConfirm = { onIntent(FeedIntent.ConfirmDelete) },
    )
}
