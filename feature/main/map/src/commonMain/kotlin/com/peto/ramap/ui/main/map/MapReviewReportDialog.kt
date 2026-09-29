package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.review.ReviewReportDialog
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapUiState

@Composable
internal fun MapReviewReportDialog(
    state: MapUiState,
    onIntent: (MapIntent) -> Unit,
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
            if (!state.isReportingReview) onIntent(MapIntent.OnReviewReportDismissed)
        },
        onSubmit = { reason, reportDetails ->
            onIntent(MapIntent.OnReviewReportSubmitted(reason, reportDetails))
        },
    )
}
