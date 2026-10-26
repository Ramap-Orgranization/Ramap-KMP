package com.peto.ramap.ui.main.community.contract

import com.peto.ramap.domain.model.community.CommunityFeedCursor
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class CommunityUiState(
    val reviews: List<ProfileReview> = emptyList(),
    val nextCursor: CommunityFeedCursor? = null,
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
) : LoadableState<CommunityUiState> {
    val refreshing: Boolean get() = loadState.isLoading(CommunityLoadKey.Page)
    val appending: Boolean get() = loadState.isLoading(CommunityLoadKey.Append)
    val reporting: Boolean get() = loadState.isLoading(CommunityLoadKey.Report)
    val unblocking: Boolean get() = loadState.isLoading(CommunityLoadKey.Unblock)

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

    override fun withLoadingState(loadState: LoadState): CommunityUiState = copy(loadState = loadState)

    fun clearAccess(): CommunityUiState = copy(revealed = emptyMap(), revealingId = null, reportReview = null, unblockReview = null, deleteReview = null)
}
