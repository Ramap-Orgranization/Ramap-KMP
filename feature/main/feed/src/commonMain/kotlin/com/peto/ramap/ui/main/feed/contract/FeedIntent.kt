package com.peto.ramap.ui.main.feed.contract

import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.base.Intent

sealed interface FeedIntent : Intent {
    data class SelectSort(
        val sort: ReviewFeedSort,
    ) : FeedIntent

    data object Refresh : FeedIntent

    data object LoadMore : FeedIntent

    data object Retry : FeedIntent

    data class ToggleLike(
        val review: Review,
    ) : FeedIntent

    data class Reveal(
        val review: Review,
    ) : FeedIntent

    data class RequestUnblock(
        val review: Review,
    ) : FeedIntent

    data object ConfirmUnblock : FeedIntent

    data object DismissUnblock : FeedIntent

    data class RequestReport(
        val review: Review,
    ) : FeedIntent

    data class SubmitReport(
        val reason: ReportReason,
        val details: String,
    ) : FeedIntent

    data object DismissReport : FeedIntent

    data class RequestDelete(
        val review: Review,
    ) : FeedIntent

    data object ConfirmDelete : FeedIntent

    data object DismissDelete : FeedIntent
}
