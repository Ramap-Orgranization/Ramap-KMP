package com.peto.ramap.ui.shop.review.contract

import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.base.Intent

sealed interface ReviewsIntent : Intent {
    data class OnShopSelected(
        val shopId: String,
    ) : ReviewsIntent

    data object OnLoadMore : ReviewsIntent

    data object OnRetry : ReviewsIntent

    data class OnReviewLikeToggled(
        val review: Review,
    ) : ReviewsIntent

    data class OnReviewDeleted(
        val review: Review,
    ) : ReviewsIntent

    data class OnReviewReportRequested(
        val review: Review,
    ) : ReviewsIntent

    data class OnReviewReportSubmitted(
        val reason: ReportReason,
        val description: String,
    ) : ReviewsIntent

    data object OnReviewReportDismissed : ReviewsIntent

    data class OnBlockedReviewViewRequested(
        val review: Review,
    ) : ReviewsIntent

    data class OnBlockedReviewUnblockRequested(
        val review: Review,
    ) : ReviewsIntent

    data object OnBlockedReviewUnblockConfirmed : ReviewsIntent

    data object OnBlockedReviewUnblockDismissed : ReviewsIntent
}
