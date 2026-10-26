package com.peto.ramap.ui.main.feed.contract

import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class FeedUiState(
    val reviews: List<ProfileReview> = emptyList(),
    val sort: ReviewFeedSort = ReviewFeedSort.LATEST,
    val filter: ReviewFeedFilter = ReviewFeedFilter.ALL,
    val nextCursor: ReviewFeedCursor? = null,
    val currentUserId: String? = null,
    val sessionResolved: Boolean = false,
    val hasLoaded: Boolean = false,
    val failed: Boolean = false,
    val appendFailed: Boolean = false,
    val revealed: Map<String, Review> = emptyMap(),
    val revealingId: String? = null,
    val actingId: String? = null,
    val reportReview: Review? = null,
    val unblockReview: Review? = null,
    val deleteReview: Review? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<FeedUiState> {
    val refreshing: Boolean get() = loadState.isLoading(FeedLoadKey.Page)
    val appending: Boolean get() = loadState.isLoading(FeedLoadKey.Append)
    val reporting: Boolean get() = loadState.isLoading(FeedLoadKey.Report)
    val unblocking: Boolean get() = loadState.isLoading(FeedLoadKey.Unblock)

    val isLoadingInitial: Boolean
        get() = !sessionResolved || (!hasLoaded && !failed)

    val isEmpty: Boolean
        get() = hasLoaded && reviews.isEmpty() && !failed

    val isRefreshing: Boolean
        get() = refreshing && hasLoaded

    val hasError: Boolean
        get() = failed

    val hasAppendError: Boolean
        get() = appendFailed

    override fun withLoadingState(loadState: LoadState): FeedUiState = copy(loadState = loadState)

    fun clearAccess(): FeedUiState = copy(revealed = emptyMap(), revealingId = null, reportReview = null, unblockReview = null, deleteReview = null)
}
