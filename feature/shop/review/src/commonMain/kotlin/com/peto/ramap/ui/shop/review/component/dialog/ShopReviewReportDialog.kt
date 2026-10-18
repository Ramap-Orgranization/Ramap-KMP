package com.peto.ramap.ui.shop.review.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.dialog.ReviewReportDialog
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState

@Composable
fun ShopReviewReportDialog(
    state: ReviewsUiState,
    onIntent: (ReviewsIntent) -> Unit,
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
            if (!state.isReportingReview) onIntent(ReviewsIntent.OnReviewReportDismissed)
        },
        onSubmit = { reason, reportDetails ->
            onIntent(ReviewsIntent.OnReviewReportSubmitted(reason, reportDetails))
        },
    )
}
