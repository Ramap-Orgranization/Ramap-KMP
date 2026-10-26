package com.peto.ramap.ui.main.community.contract

import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.base.Intent

sealed interface CommunityIntent : Intent {
    data object Refresh : CommunityIntent

    data object LoadMore : CommunityIntent

    data object Retry : CommunityIntent

    data class ToggleLike(
        val review: Review,
    ) : CommunityIntent

    data class Reveal(
        val review: Review,
    ) : CommunityIntent

    data class RequestUnblock(
        val review: Review,
    ) : CommunityIntent

    data object ConfirmUnblock : CommunityIntent

    data object DismissUnblock : CommunityIntent

    data class RequestReport(
        val review: Review,
    ) : CommunityIntent

    data class SubmitReport(
        val reason: ReportReason,
        val details: String,
    ) : CommunityIntent

    data object DismissReport : CommunityIntent

    data class RequestDelete(
        val review: Review,
    ) : CommunityIntent

    data object ConfirmDelete : CommunityIntent

    data object DismissDelete : CommunityIntent
}
