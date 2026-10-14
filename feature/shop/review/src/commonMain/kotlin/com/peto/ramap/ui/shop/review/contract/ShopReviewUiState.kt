package com.peto.ramap.ui.shop.review.contract

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ShopReviewUiState(
    val shopId: String? = null,
    val page: ShopReviewsPage = ShopReviewsPage(emptyList(), 0),
    val hasLoaded: Boolean = false,
    val hasReviewLoadFailure: Boolean = false,
    val hasPageLoadFailure: Boolean = false,
    val currentUserId: String? = null,
    val currentProfileIsPublic: Boolean? = null,
    val isLoggedIn: Boolean = false,
    val actingReviewId: String? = null,
    val revealedBlockedReviews: Map<String, Review> = emptyMap(),
    val revealingBlockedReviewId: String? = null,
    val pendingUnblockReview: Review? = null,
    val reportReview: Review? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ShopReviewUiState> {
    val reviews: List<Review> get() = page.reviews
    val reviewCount: Int get() = page.totalCount
    val hasMoreReviews: Boolean get() = hasLoaded && page.hasMore
    val isLoading: Boolean get() = loadState.isLoading(ShopReviewLoadKey.Reviews)
    val isLoadingMoreReviews: Boolean get() = loadState.isLoading(ShopReviewLoadKey.ReviewsPage)
    val isReportingReview: Boolean get() = loadState.isLoading(ShopReviewLoadKey.ReviewReport)
    val isUnblockingReview: Boolean get() = loadState.isLoading(ShopReviewLoadKey.BlockedReviewUnblock)

    fun clearAccess(): ShopReviewUiState = copy(revealedBlockedReviews = emptyMap(), revealingBlockedReviewId = null, pendingUnblockReview = null)

    override fun withLoadingState(loadState: LoadState): ShopReviewUiState = copy(loadState = loadState)
}
