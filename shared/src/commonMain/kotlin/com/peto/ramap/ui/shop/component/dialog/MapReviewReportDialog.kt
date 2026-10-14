package com.peto.ramap.ui.shop.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.review.ReviewReportDialog
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.ui.shop.review.contract.ShopReviewIntent
import com.peto.ramap.ui.shop.review.contract.ShopReviewUiState

@Composable
fun MapReviewReportDialog(
    state: ShopReviewUiState,
    onIntent: (ShopReviewIntent) -> Unit,
) {
    var selectedReason by remember(state.reportReview?.id) { mutableStateOf<ReportReason?>(null) }
    var details by remember(state.reportReview?.id) { mutableStateOf("") }

    ReviewReportDialog(
        visible = state.reportReview != null,
        selectedReason = selectedReason,
        onReasonSelected = {
            selectedReason = it
            details = details.take(it.maxDetailsLength)
        },
        details = details,
        onDetailsChanged = { details = it },
        isActing = state.isReportingReview,
        onDismissRequest = {
            if (!state.isReportingReview) onIntent(ShopReviewIntent.OnReviewReportDismissed)
        },
        onSubmit = { reason, reportDetails ->
            onIntent(ShopReviewIntent.OnReviewReportSubmitted(reason, reportDetails))
        },
    )
}
