package com.peto.ramap.ui.review.my.contract

import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class MyReviewsUiState(
    val userId: String? = null,
    val filter: MyReviewVisibility = MyReviewVisibility.ALL,
    val reviews: List<MyReview> = emptyList(),
    val totalCount: Int = 0,
    val publicCount: Int = 0,
    val privateCount: Int = 0,
    val profileIsPublic: Boolean = true,
    val loaded: Boolean = false,
    val failed: Boolean = false,
    val failedReset: Boolean = false,
    val hasMore: Boolean = false,
    val actingReviewId: String? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<MyReviewsUiState> {
    val loading: Boolean get() = loadState.isLoading(MyReviewsLoadKey.Page)

    val showsInitialLoading: Boolean get() = loading && reviews.isEmpty()
    val showsAppendLoading: Boolean get() = loading && reviews.isNotEmpty()
    val showsEmptyReviews: Boolean get() = !loading && !failed && reviews.isEmpty() && loaded

    override fun withLoadingState(loadState: LoadState): MyReviewsUiState = copy(loadState = loadState)
}
