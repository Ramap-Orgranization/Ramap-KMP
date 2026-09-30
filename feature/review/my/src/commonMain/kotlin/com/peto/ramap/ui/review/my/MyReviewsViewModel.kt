package com.peto.ramap.ui.review.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.my.contract.MyReviewsIntent
import com.peto.ramap.ui.review.my.contract.MyReviewsLoadKey
import com.peto.ramap.ui.review.my.contract.MyReviewsSideEffect
import com.peto.ramap.ui.review.my.contract.MyReviewsUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_delete_failed

class MyReviewsViewModel(
    private val reviews: ReviewRepository,
    private val profiles: ProfileRepository,
) : BaseViewModel<MyReviewsUiState, MyReviewsIntent, MyReviewsSideEffect>(MyReviewsUiState()) {
    init {
        observeSessionUserIds()
        observeReviewChanges()
    }

    private fun observeSessionUserIds() {
        viewModelScope.launch {
            profiles.sessionUserIds.distinctUntilChanged().collect { userId ->
                cancelTask(PAGE_TASK)
                reduce { MyReviewsUiState(userId = userId) }
                if (userId != null) loadPage(reset = true)
            }
        }
    }

    private fun observeReviewChanges() {
        viewModelScope.launch {
            reviews.observeChanges().collect {
                if (currentState.userId != null) loadPage(reset = true)
            }
        }
    }

    override suspend fun handleIntent(intent: MyReviewsIntent) {
        when (intent) {
            is MyReviewsIntent.SelectFilter -> handleSelectFilter(intent.filter)
            MyReviewsIntent.Retry -> handleRetry()
            MyReviewsIntent.LoadMore -> handleLoadMore()
            is MyReviewsIntent.DeleteReview -> handleDeleteReview(intent.reviewId)
        }
    }

    private fun handleSelectFilter(filter: MyReviewVisibility) {
        if (filter == currentState.filter) return
        cancelTask(PAGE_TASK)
        reduce {
            copy(
                filter = filter,
                reviews = emptyList(),
                loaded = false,
                failed = false,
                hasMore = false,
            )
        }
        loadPage(reset = true)
    }

    private fun handleRetry() {
        loadPage(reset = currentState.failedReset || currentState.reviews.isEmpty())
    }

    private fun handleLoadMore() {
        if (currentState.hasMore && !currentState.loading && !currentState.failed) {
            loadPage(reset = false)
        }
    }

    private fun handleDeleteReview(reviewId: String) {
        deleteReview(reviewId)
    }

    private fun deleteReview(reviewId: String) {
        val review = currentState.reviews.firstOrNull { it.id == reviewId } ?: return
        val actingUserId = currentState.userId ?: return
        if (currentState.actingReviewId != null) return

        launchResultTask(
            taskKey = DELETE_TASK,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingReviewId = reviewId) },
            request = { reviews.deleteReview(reviewId) },
            onSuccess = {
                if (currentState.userId == actingUserId) {
                    handleDeleteSuccess(reviewId = reviewId, review = review)
                }
            },
            onError = {
                if (currentState.userId == actingUserId) {
                    handleDeleteError()
                }
            },
            onFinish = {
                if (actingReviewId == reviewId) copy(actingReviewId = null) else this
            },
        )
    }

    private fun handleDeleteSuccess(
        reviewId: String,
        review: MyReview,
    ) {
        reduce { applyDeleteSuccess(reviewId = reviewId, review = review) }
    }

    private fun MyReviewsUiState.applyDeleteSuccess(
        reviewId: String,
        review: MyReview,
    ): MyReviewsUiState {
        val updatedReviews = this.reviews.filterNot { it.id == reviewId }
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

    private suspend fun handleDeleteError() {
        postSideEffect(
            MyReviewsSideEffect.ShowToast(
                ToastData(Res.string.review_delete_failed, ToastType.ERROR),
            ),
        )
    }

    private fun loadPage(reset: Boolean) {
        val userId = currentState.userId ?: return
        val filter = currentState.filter
        val offset = if (reset) 0L else currentState.reviews.size.toLong()
        val targetSize = if (reset) maxOf(PAGE_SIZE, currentState.reviews.size) else PAGE_SIZE

        launchResultTask(
            taskKey = PAGE_TASK,
            loadKey = MyReviewsLoadKey.Page,
            onStart = { copy(failed = false, failedReset = false) },
            request = { fetchExtent(offset, filter, targetSize) },
            onSuccess = { page ->
                if (userId == currentState.userId && filter == currentState.filter) {
                    handlePageSuccess(page = page, reset = reset)
                }
            },
            onError = { handlePageError(reset = reset) },
        )
    }

    private fun handlePageSuccess(
        page: MyReviewsPage,
        reset: Boolean,
    ) {
        reduce { applyPageSuccess(page = page, reset = reset) }
    }

    private fun MyReviewsUiState.applyPageSuccess(
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

    private fun handlePageError(reset: Boolean) {
        reduce { copy(failed = true, failedReset = reset, loaded = true) }
    }

    private suspend fun fetchExtent(
        offset: Long,
        filter: MyReviewVisibility,
        targetSize: Int,
    ): RamapResult<MyReviewsPage> {
        val gathered = mutableListOf<MyReview>()
        var latest: MyReviewsPage? = null
        while (gathered.size < targetSize) {
            when (val result = reviews.fetchMyReviews(offset + gathered.size, filter)) {
                is RamapResult.Error -> return result
                is RamapResult.Success -> {
                    latest = result.data
                    gathered += result.data.reviews
                    if (result.data.reviews.size < PAGE_SIZE) break
                }
            }
        }
        return RamapResult.Success(requireNotNull(latest).copy(reviews = gathered))
    }

    private companion object {
        const val PAGE_TASK = "my-reviews-page"
        const val DELETE_TASK = "my-reviews-delete"
        const val PAGE_SIZE = 30
    }
}
