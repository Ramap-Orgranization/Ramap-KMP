package com.peto.ramap.ui.shop.review.contract

import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.base.Intent

sealed interface ShopReviewIntent : Intent {
    data class OnShopSelected(
        val shopId: String,
    ) : ShopReviewIntent

    data object OnDismissed : ShopReviewIntent

    data object OnLoadMore : ShopReviewIntent

    data object OnRetry : ShopReviewIntent

    data class OnReviewLikeToggled(
        val review: Review,
    ) : ShopReviewIntent

    data class OnReviewDeleted(
        val review: Review,
    ) : ShopReviewIntent

    data class OnReviewReportRequested(
        val review: Review,
    ) : ShopReviewIntent

    data class OnReviewReportSubmitted(
        val reason: ReportReason,
        val description: String,
    ) : ShopReviewIntent

    data object OnReviewReportDismissed : ShopReviewIntent

    data class OnBlockedReviewViewRequested(
        val review: Review,
    ) : ShopReviewIntent

    data class OnBlockedReviewUnblockRequested(
        val review: Review,
    ) : ShopReviewIntent

    data object OnBlockedReviewUnblockConfirmed : ShopReviewIntent

    data object OnBlockedReviewUnblockDismissed : ShopReviewIntent
}
