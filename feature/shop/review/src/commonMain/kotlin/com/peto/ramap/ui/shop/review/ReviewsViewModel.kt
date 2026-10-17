package com.peto.ramap.ui.shop.review

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import com.peto.ramap.ui.shop.review.contract.ReviewsLoadKey
import com.peto.ramap.ui.shop.review.contract.ReviewsSideEffect
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_delete_failed
import ramap.shared.generated.resources.review_like_failed

class ReviewsViewModel(
    private val reviewRepository: ReviewRepository,
    private val communityRepository: CommunityRepository,
    private val profileRepository: ProfileRepository,
    private val loginRepository: LoginRepository,
    private val reviewChanges: ReviewChangeStore,
    private val reviewChangeReducer: ReviewChangeReducer,
    private val fetchReviews: FeatchShopReviewsUseCase,
) : BaseViewModel<ReviewsUiState, ReviewsIntent, ReviewsSideEffect>(ReviewsUiState()) {
    private var revision = reviewChanges.journal.value.revision

    init {
        viewModelScope.launch { observeChanges() }
        viewModelScope.launch { awaitUser() }
        viewModelScope.launch {
            loginRepository.session.first { it == LoginSessionState.AUTHENTICATED }
            reduce { copy(isLoggedIn = true) }
        }
    }

    override suspend fun handleIntent(intent: ReviewsIntent) {
        when (intent) {
            is ReviewsIntent.OnShopSelected -> selectShop(intent.shopId)
            ReviewsIntent.OnDismissed -> {
                cancelReviewTasks()
                reduce { copy(shopId = null, page = ShopReviewsPage(emptyList(), 0), hasLoaded = false, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
            }
            ReviewsIntent.OnRetry -> loadPage(append = false)
            ReviewsIntent.OnLoadMore -> if (currentState.hasMoreReviews && !currentState.isLoadingMoreReviews) loadPage(append = true)
            is ReviewsIntent.OnReviewLikeToggled -> toggleLike(intent.review)
            is ReviewsIntent.OnReviewDeleted -> deleteReview(intent.review)
            is ReviewsIntent.OnReviewReportRequested -> requestReport(intent.review)
            is ReviewsIntent.OnReviewReportSubmitted -> submitReport(intent.reason, intent.description)
            ReviewsIntent.OnReviewReportDismissed -> {
                cancelTask(REPORT_TASK_KEY)
                reduce { copy(reportReview = null) }
            }
            is ReviewsIntent.OnBlockedReviewViewRequested -> revealBlockedReview(intent.review)
            is ReviewsIntent.OnBlockedReviewUnblockRequested -> requestUnblock(intent.review)
            ReviewsIntent.OnBlockedReviewUnblockConfirmed -> unblockAuthor()
            ReviewsIntent.OnBlockedReviewUnblockDismissed -> if (!currentState.isUnblockingReview) reduce { copy(pendingUnblockReview = null) }
        }
    }

    private fun selectShop(shopId: String) {
        if (currentState.shopId == shopId) return
        cancelReviewTasks()
        val cached = fetchReviews.findCached(shopId, currentState.currentUserId)
        reduce { copy(shopId = shopId, page = cached ?: ShopReviewsPage(emptyList(), 0), hasLoaded = cached != null, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
        loadPage(append = false)
    }

    private fun loadPage(append: Boolean) {
        val shopId = currentState.shopId ?: return
        val viewer = currentState.currentUserId
        if (viewer == null && loginRepository.hasSession()) return
        val retained = currentState.page
        launchResultTask(
            taskKey = REVIEWS_TASK_KEY,
            loadKey = if (append) ReviewsLoadKey.ReviewsPage else ReviewsLoadKey.Reviews,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(hasReviewLoadFailure = append && hasReviewLoadFailure, hasPageLoadFailure = false) },
            request = { fetchReviews(shopId, viewer, retained, append) },
            onSuccess = { page ->
                if (!currentCoroutineContext().isActive) return@launchResultTask
                reduce { copy(page = page, hasLoaded = true, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
                fetchReviews.savePage(shopId, viewer, page)
            },
            onError = {
                if (!currentCoroutineContext().isActive) return@launchResultTask
                reduce { copy(hasReviewLoadFailure = !append, hasPageLoadFailure = append) }
                if (append) showToast(Res.string.data_load_failure_message, ToastType.ERROR)
            },
        )
    }

    private suspend fun observeChanges() {
        reviewChanges.journal.collect { journal ->
            if (journal.userId != currentState.currentUserId) return@collect
            if (journal.hasGapAfter(revision)) {
                revision = journal.revision
                loadPage(append = false)
                return@collect
            }
            for (record in journal.changesAfter(revision)) {
                val change = record.change
                val state = currentState
                val shopId = state.shopId
                if (shopId != null) {
                    val updated = reviewChangeReducer.updateShopPage(state.page, change, shopId, state.currentUserId, state.hasMoreReviews)
                    reduce { copy(page = updated, currentProfileIsPublic = (change as? ReviewChange.Written)?.takeIf { it.userId == currentUserId }?.counts?.profileIsPublic ?: currentProfileIsPublic) }
                    if (change is ReviewChange.AccessChanged && updated.reviews != state.reviews) {
                        cancelTask(REVEAL_TASK_KEY)
                        reduce { clearAccess() }
                        loadPage(append = false)
                    }
                }
                revision = record.revision
            }
        }
    }

    private suspend fun awaitUser() {
        val viewer = profileRepository.sessionUserId.filterNotNull().first()
        cancelReviewTasks()
        val journal = reviewChanges.journal.first { it.userId == viewer }
        revision = journal.revision
        fetchReviews.clearCache()
        reduce { copy(currentUserId = viewer, page = ShopReviewsPage(emptyList(), 0), hasLoaded = false, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
        loadProfile(viewer)
        loadPage(append = false)
    }

    private fun loadProfile(viewer: String) {
        viewModelScope.launch {
            val result = profileRepository.fetchMyProfile()
            val profile = (result as? RamapResult.Success)?.data ?: return@launch
            if (isActive && profile.userId == viewer) reduce { copy(currentProfileIsPublic = profile.isPublic) }
        }
    }

    private fun cancelReviewTasks() {
        for (key in listOf(REVIEWS_TASK_KEY, ACTION_TASK_KEY, REPORT_TASK_KEY, REVEAL_TASK_KEY, UNBLOCK_TASK_KEY)) cancelTask(key)
        reduce { clearAccess().copy(actingReviewId = null, reportReview = null) }
    }

    private fun currentBlocked(review: Review): Review? = currentState.reviews.firstOrNull { it.isBlockedVersionOf(review) }

    private fun revealBlockedReview(review: Review) {
        if (currentState.isUnblockingReview || currentState.isLoading) return
        val blocked = currentBlocked(review) ?: return
        if (currentState.currentUserId == null) return
        if (currentState.revealedBlockedReviews.containsKey(blocked.id)) return
        launchResultTask(
            taskKey = REVEAL_TASK_KEY,
            loadKey = ReviewsLoadKey.BlockedReviewReveal,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(revealingBlockedReviewId = blocked.id) },
            onFinish = { copy(revealingBlockedReviewId = null) },
            request = { reviewRepository.fetchBlockedShopReviewOnce(blocked.id) },
            onSuccess = { revealed ->
                if (!currentCoroutineContext().isActive || currentBlocked(blocked) == null) return@launchResultTask
                if (revealed == null || !revealed.isBlockedVersionOf(blocked)) {
                    showToast(Res.string.review_action_failed, ToastType.ERROR)
                    return@launchResultTask
                }
                reduce { copy(revealedBlockedReviews = revealedBlockedReviews + (revealed.id to revealed)) }
            },
            onError = { if (currentCoroutineContext().isActive) showToast(Res.string.review_action_failed, ToastType.ERROR) },
        )
    }

    private fun requestUnblock(review: Review) {
        val blocked = currentBlocked(review) ?: return
        if (currentState.currentUserId == null || currentState.isUnblockingReview) return
        reduce { copy(pendingUnblockReview = blocked) }
    }

    private fun unblockAuthor() {
        val review = currentState.pendingUnblockReview ?: return
        if (currentState.currentUserId == null) return
        if (currentBlocked(review) == null || !review.allowsAuthorUnblocking()) return
        launchResultTask(
            taskKey = UNBLOCK_TASK_KEY,
            loadKey = ReviewsLoadKey.BlockedReviewUnblock,
            policy = TaskPolicy.IgnoreNew,
            request = { communityRepository.unblockUser(review.author.userId) },
            onSuccess = {
                if (!currentCoroutineContext().isActive) return@launchResultTask
                cancelTask(REVEAL_TASK_KEY)
                reduce { clearAccess() }
                showToast(Res.string.review_action_success)
                loadPage(append = false)
            },
            onError = {
                if (currentCoroutineContext().isActive) {
                    reduce { copy(pendingUnblockReview = null) }
                    showToast(Res.string.review_action_failed, ToastType.ERROR)
                }
            },
        )
    }

    private fun toggleLike(review: Review) {
        if (review.shopId != currentState.shopId || !review.allowsLikeFrom(currentState.currentUserId)) return
        if (currentState.currentUserId == null) {
            trySideEffect(ReviewsSideEffect.ShowLoginGuide)
            return
        }
        val next = review.toggledLike()
        launchOptimisticTask(
            taskKey = ACTION_TASK_KEY,
            loadKey = ReviewsLoadKey.ReviewAction,
            isUpdating = { it.actingReviewId != null },
            onStart = { copy(actingReviewId = review.id, page = reviewChangeReducer.updateShopPage(page, ReviewChange.Liked(review.id, next), review.shopId, currentUserId, hasMoreReviews)) },
            onFinish = { copy(actingReviewId = null) },
            rollback = {
                copy(actingReviewId = null, page = reviewChangeReducer.updateShopPage(page, ReviewChange.Liked(review.id, ReviewLike(review.likeCount, review.isLiked)), review.shopId, currentUserId, hasMoreReviews))
            },
            request = { reviewRepository.setReviewLike(review.id, next.isLiked) },
            onSuccess = { like -> if (currentCoroutineContext().isActive) updateLike(review.id, like) },
            onError = { if (currentCoroutineContext().isActive) showToast(Res.string.review_like_failed, ToastType.ERROR) },
        )
    }

    private fun updateLike(
        reviewId: String,
        like: ReviewLike,
    ) {
        val shopId = currentState.shopId ?: return
        reduce { copy(page = reviewChangeReducer.updateShopPage(page, ReviewChange.Liked(reviewId, like), shopId, currentUserId, hasMoreReviews)) }
    }

    private fun deleteReview(review: Review) {
        if (review.shopId != currentState.shopId || !review.allowsDeletionBy(currentState.currentUserId)) return
        launchResultTask(
            taskKey = ACTION_TASK_KEY,
            loadKey = ReviewsLoadKey.ReviewAction,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingReviewId = review.id) },
            onFinish = { copy(actingReviewId = null) },
            request = { reviewRepository.deleteReview(review.id) },
            onError = { if (currentCoroutineContext().isActive) showToast(Res.string.review_delete_failed, ToastType.ERROR) },
        )
    }

    private fun requestReport(review: Review) {
        if (!currentState.isLoggedIn) {
            trySideEffect(ReviewsSideEffect.ShowLoginGuide)
            return
        }
        if (review.shopId != currentState.shopId || !review.allowsReportFrom(currentState.currentUserId) || currentState.isReportingReview) return
        reduce { copy(reportReview = review) }
    }

    private fun submitReport(
        reason: ReportReason,
        description: String,
    ) {
        val review = currentState.reportReview ?: return
        val viewer = currentState.currentUserId ?: return
        if (!currentState.isLoggedIn || review.shopId != currentState.shopId || !review.allowsReportFrom(viewer) || !reason.isValidDetails(description)) return
        launchResultTask(
            taskKey = REPORT_TASK_KEY,
            loadKey = ReviewsLoadKey.ReviewReport,
            policy = TaskPolicy.IgnoreNew,
            request = { communityRepository.reportReview(review.id, reason, description) },
            onSuccess = {
                if (currentCoroutineContext().isActive && currentState.reportReview?.id == review.id) {
                    reduce { copy(reportReview = null) }
                    showToast(Res.string.review_action_success, ToastType.SUCCESS)
                }
            },
            onError = { if (currentCoroutineContext().isActive && currentState.reportReview?.id == review.id) showToast(Res.string.review_action_failed, ToastType.ERROR) },
        )
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(ReviewsSideEffect.ShowToast(message, type))
    }

    companion object {
        private const val REVIEWS_TASK_KEY = "shop_reviews"
        private const val ACTION_TASK_KEY = "shop_review_action"
        private const val REPORT_TASK_KEY = "shop_review_report"
        private const val REVEAL_TASK_KEY = "shop_review_reveal"
        private const val UNBLOCK_TASK_KEY = "shop_review_unblock"
    }
}
