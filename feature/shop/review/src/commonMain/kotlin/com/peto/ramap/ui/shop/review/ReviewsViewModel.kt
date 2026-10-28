package com.peto.ramap.ui.shop.review

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.UserModerationRepository
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
import ramap.shared.generated.resources.login_required_review_like_description
import ramap.shared.generated.resources.login_required_review_report_description
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_delete_failed
import ramap.shared.generated.resources.review_like_failed
import ramap.shared.generated.resources.review_report_failed
import ramap.shared.generated.resources.review_unblock_failed
import ramap.shared.generated.resources.review_unblock_success

class ReviewsViewModel(
    private val reviewRepository: ReviewRepository,
    private val userModerationRepository: UserModerationRepository,
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
            ReviewsIntent.OnRetry -> loadPage(append = false)
            ReviewsIntent.OnLoadMore -> {
                val canLoadMore = currentState.hasMoreReviews && !currentState.isLoadingMoreReviews
                if (canLoadMore) loadPage(append = true)
            }
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
            ReviewsIntent.OnBlockedReviewUnblockDismissed -> {
                val isNotUnblocking = !currentState.isUnblockingReview
                if (isNotUnblocking) reduce { copy(pendingUnblockReview = null) }
            }
        }
    }

    private fun selectShop(shopId: String) {
        val isSameShopSelected = currentState.shopId == shopId
        if (isSameShopSelected) return
        cancelReviewTasks()
        val cached = fetchReviews.findCached(shopId, currentState.currentUserId)
        val hasCachedPage = cached != null
        reduce { copy(shopId = shopId, page = cached ?: ShopReviewsPage(emptyList(), 0), hasLoaded = hasCachedPage, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
        loadPage(append = false)
    }

    private fun loadPage(append: Boolean) {
        val shopId = currentState.shopId ?: return
        val viewer = currentState.currentUserId
        val isViewerPendingLogin = viewer == null && loginRepository.hasSession()
        if (isViewerPendingLogin) return
        val retained = currentState.page
        launchResultTask(
            taskKey = REVIEWS_TASK_KEY,
            loadKey = if (append) ReviewsLoadKey.ReviewsPage else ReviewsLoadKey.Reviews,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(hasReviewLoadFailure = append && hasReviewLoadFailure, hasPageLoadFailure = false) },
            request = { fetchReviews(shopId, viewer, retained, append) },
            onSuccess = { page ->
                val isInactive = !currentCoroutineContext().isActive
                if (isInactive) return@launchResultTask
                reduce { copy(page = page, hasLoaded = true, hasReviewLoadFailure = false, hasPageLoadFailure = false) }
                fetchReviews.savePage(shopId, viewer, page)
            },
            onError = {
                val isInactive = !currentCoroutineContext().isActive
                if (isInactive) return@launchResultTask
                reduce { copy(hasReviewLoadFailure = !append, hasPageLoadFailure = append) }
                if (append) showToast(Res.string.data_load_failure_message, ToastType.ERROR)
            },
        )
    }

    private suspend fun observeChanges() {
        reviewChanges.journal.collect { journal ->
            val isUserMismatch = journal.userId != currentState.currentUserId
            if (isUserMismatch) return@collect
            val hasGap = journal.hasGapAfter(revision)
            if (hasGap) {
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
                    val isAccessChangedAndUpdated = change is ReviewChange.AccessChanged && updated.reviews != state.reviews
                    if (isAccessChangedAndUpdated) {
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
            val isCurrentViewer = isActive && profile.userId == viewer
            if (isCurrentViewer) reduce { copy(currentProfileIsPublic = profile.isPublic) }
        }
    }

    private fun cancelReviewTasks() {
        for (key in listOf(REVIEWS_TASK_KEY, ACTION_TASK_KEY, REPORT_TASK_KEY, REVEAL_TASK_KEY, UNBLOCK_TASK_KEY)) cancelTask(key)
        reduce { clearAccess().copy(actingReviewId = null, reportReview = null) }
    }

    private fun currentBlocked(review: Review): Review? = currentState.reviews.firstOrNull { it.isBlockedVersionOf(review) }

    private fun revealBlockedReview(review: Review) {
        val cannotReveal = currentState.isUnblockingReview || currentState.isLoading
        if (cannotReveal) return
        val blocked = currentBlocked(review) ?: return
        val isLoggedOut = currentState.currentUserId == null
        if (isLoggedOut) return
        val isAlreadyRevealed = currentState.revealedBlockedReviews.containsKey(blocked.id)
        if (isAlreadyRevealed) return
        launchResultTask(
            taskKey = REVEAL_TASK_KEY,
            loadKey = ReviewsLoadKey.BlockedReviewReveal,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(revealingBlockedReviewId = blocked.id) },
            onFinish = { copy(revealingBlockedReviewId = null) },
            request = { reviewRepository.fetchBlockedShopReviewOnce(blocked.id) },
            onSuccess = { revealed ->
                val isInactiveOrMissing = !currentCoroutineContext().isActive || currentBlocked(blocked) == null
                if (isInactiveOrMissing) return@launchResultTask
                val isInvalidRevealedReview = revealed == null || !revealed.isBlockedVersionOf(blocked)
                if (isInvalidRevealedReview) {
                    showToast(Res.string.review_action_failed, ToastType.ERROR)
                    return@launchResultTask
                }
                reduce { copy(revealedBlockedReviews = revealedBlockedReviews + (revealed.id to revealed)) }
            },
            onError = {
                val isActiveContext = currentCoroutineContext().isActive
                if (isActiveContext) showToast(Res.string.review_action_failed, ToastType.ERROR)
            },
        )
    }

    private fun requestUnblock(review: Review) {
        val blocked = currentBlocked(review) ?: return
        val cannotRequestUnblock = currentState.currentUserId == null || currentState.isUnblockingReview
        if (cannotRequestUnblock) return
        reduce { copy(pendingUnblockReview = blocked) }
    }

    private fun unblockAuthor() {
        val review = currentState.pendingUnblockReview ?: return
        val isLoggedOut = currentState.currentUserId == null
        if (isLoggedOut) return
        val cannotUnblock = currentBlocked(review) == null || !review.allowsAuthorUnblocking()
        if (cannotUnblock) return
        launchResultTask(
            taskKey = UNBLOCK_TASK_KEY,
            loadKey = ReviewsLoadKey.BlockedReviewUnblock,
            policy = TaskPolicy.IgnoreNew,
            request = { userModerationRepository.unblockUser(review.author.userId) },
            onSuccess = {
                val isInactive = !currentCoroutineContext().isActive
                if (isInactive) return@launchResultTask
                cancelTask(REVEAL_TASK_KEY)
                reduce { clearAccess() }
                showToast(Res.string.review_unblock_success)
                loadPage(append = false)
            },
            onError = {
                val isActiveContext = currentCoroutineContext().isActive
                if (isActiveContext) {
                    reduce { copy(pendingUnblockReview = null) }
                    showToast(Res.string.review_unblock_failed, ToastType.ERROR)
                }
            },
        )
    }

    private fun toggleLike(review: Review) {
        val isDifferentShop = review.shopId != currentState.shopId
        val cannotLike = !review.allowsLikeFrom(currentState.currentUserId)
        if (isDifferentShop || cannotLike) return
        val isLoggedOut = currentState.currentUserId == null
        if (isLoggedOut) {
            trySideEffect(ReviewsSideEffect.ShowLoginGuide(Res.string.login_required_review_like_description))
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
            onSuccess = { like ->
                val isActiveContext = currentCoroutineContext().isActive
                if (isActiveContext) updateLike(review.id, like)
            },
            onError = {
                val isActiveContext = currentCoroutineContext().isActive
                if (isActiveContext) showToast(Res.string.review_like_failed, ToastType.ERROR)
            },
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
        val isDifferentShop = review.shopId != currentState.shopId
        val cannotDelete = !review.allowsDeletionBy(currentState.currentUserId)
        if (isDifferentShop || cannotDelete) return
        launchResultTask(
            taskKey = ACTION_TASK_KEY,
            loadKey = ReviewsLoadKey.ReviewAction,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingReviewId = review.id) },
            onFinish = { copy(actingReviewId = null) },
            request = { reviewRepository.deleteReview(review.id) },
            onError = {
                val isActiveContext = currentCoroutineContext().isActive
                if (isActiveContext) showToast(Res.string.review_delete_failed, ToastType.ERROR)
            },
        )
    }

    private fun requestReport(review: Review) {
        val isLoggedOut = !currentState.isLoggedIn
        if (isLoggedOut) {
            trySideEffect(ReviewsSideEffect.ShowLoginGuide(Res.string.login_required_review_report_description))
            return
        }
        val isDifferentShop = review.shopId != currentState.shopId
        val cannotReport = !review.allowsReportFrom(currentState.currentUserId)
        val isReportingAlready = currentState.isReportingReview
        if (isDifferentShop || cannotReport || isReportingAlready) return
        reduce { copy(reportReview = review) }
    }

    private fun submitReport(
        reason: ReportReason,
        description: String,
    ) {
        val review = currentState.reportReview ?: return
        val viewer = currentState.currentUserId ?: return
        val isLoggedOut = !currentState.isLoggedIn
        val isDifferentShop = review.shopId != currentState.shopId
        val cannotReport = !review.allowsReportFrom(viewer)
        val isInvalidDetails = !reason.isValidDetails(description)
        if (isLoggedOut || isDifferentShop || cannotReport || isInvalidDetails) return
        launchResultTask(
            taskKey = REPORT_TASK_KEY,
            loadKey = ReviewsLoadKey.ReviewReport,
            policy = TaskPolicy.IgnoreNew,
            request = { userModerationRepository.reportReview(review.id, reason, description) },
            onSuccess = {
                val isActiveTargetReview = currentCoroutineContext().isActive && currentState.reportReview?.id == review.id
                if (isActiveTargetReview) {
                    reduce { copy(reportReview = null) }
                    showToast(Res.string.review_action_success, ToastType.SUCCESS)
                }
            },
            onError = {
                val isActiveTargetReview = currentCoroutineContext().isActive && currentState.reportReview?.id == review.id
                if (isActiveTargetReview) showToast(Res.string.review_report_failed, ToastType.ERROR)
            },
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
