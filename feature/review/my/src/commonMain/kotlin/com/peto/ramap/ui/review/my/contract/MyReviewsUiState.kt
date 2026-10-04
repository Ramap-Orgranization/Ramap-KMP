package com.peto.ramap.ui.review.my.contract

import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
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

    fun applyDeleteSuccess(
        reviewId: String,
        review: MyReview,
    ): MyReviewsUiState {
        val updatedReviews = reviews.filterNot { it.id == reviewId }
        val updatedTotalCount = (totalCount - 1).coerceAtLeast(0)
        val updatedPublicCount = if (review.isPublic) (publicCount - 1).coerceAtLeast(0) else publicCount
        val updatedPrivateCount = if (review.isPublic) privateCount else (privateCount - 1).coerceAtLeast(0)
        val filteredCount =
            when (filter) {
                MyReviewVisibility.ALL -> updatedTotalCount
                MyReviewVisibility.PUBLIC -> updatedPublicCount
                MyReviewVisibility.PRIVATE -> updatedPrivateCount
            }
        return copy(
            reviews = updatedReviews,
            totalCount = updatedTotalCount,
            publicCount = updatedPublicCount,
            privateCount = updatedPrivateCount,
            hasMore = updatedReviews.size < filteredCount,
            actingReviewId = null,
        )
    }

    fun applyPageSuccess(
        page: MyReviewsPage,
        reset: Boolean,
    ): MyReviewsUiState {
        val merged = (if (reset) page.reviews else reviews + page.reviews).distinctBy { it.id }
        return copy(
            reviews = merged,
            totalCount = page.totalCount,
            publicCount = page.publicCount,
            privateCount = page.privateCount,
            profileIsPublic = page.profileIsPublic,
            loaded = true,
            hasMore =
                merged.size <
                    when (filter) {
                        MyReviewVisibility.ALL -> page.totalCount
                        MyReviewVisibility.PUBLIC -> page.publicCount
                        MyReviewVisibility.PRIVATE -> page.privateCount
                    },
        )
    }

    override fun withLoadingState(loadState: LoadState): MyReviewsUiState = copy(loadState = loadState)
}
