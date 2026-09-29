package com.peto.ramap.ui.profile.review

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.profile.review.contract.ProfileReviewIntent
import com.peto.ramap.ui.profile.review.contract.ProfileReviewLoadKey
import com.peto.ramap.ui.profile.review.contract.ProfileReviewSideEffect
import com.peto.ramap.ui.profile.review.contract.ProfileReviewTarget
import com.peto.ramap.ui.profile.review.contract.ProfileReviewUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_action_success
import kotlin.coroutines.coroutineContext

class ProfileReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val communityRepository: ReviewCommunityRepository,
    private val loginRepository: LoginRepository,
    private val profileRepository: ProfileRepository,
) : BaseViewModel<ProfileReviewUiState, ProfileReviewIntent, ProfileReviewSideEffect>(ProfileReviewUiState()) {
    private var target: ProfileReviewTarget? = null
    private var profileVisibilityJob: Job? = null

    init {
        viewModelScope.launch {
            reviewRepository.observeChanges().collect {
                if (target != null) load(reset = true, preserveVisible = true)
                refreshCurrentProfileVisibility()
            }
        }
        viewModelScope.launch {
            profileRepository
                .sessionUserIds
                .distinctUntilChanged()
                .collect { userId ->
                    profileVisibilityJob?.cancel()
                    cancelTask(FEED_TASK)
                    cancelTask(ACTION_TASK)
                    cancelTask(LIKE_TASK)
                    cancelTask(DELETE_TASK)
                    reduce { ProfileReviewUiState(isAuthenticated = userId != null) }
                    if (target != null) load(reset = true)
                }
        }
    }

    override suspend fun handleIntent(intent: ProfileReviewIntent) {
        when (intent) {
            is ProfileReviewIntent.OpenTarget -> openTarget(intent.target)
            ProfileReviewIntent.Load -> load(reset = true)
            ProfileReviewIntent.LoadMore -> load(reset = false)
            is ProfileReviewIntent.ReportReview -> openReport(intent.review.id, intent.review.author.userId, user = false)
            is ProfileReviewIntent.ToggleLike -> toggleLike(intent.review)
            is ProfileReviewIntent.ConfirmDelete -> confirmDelete(intent.review)
            ProfileReviewIntent.DeleteReview -> deleteReview()
            ProfileReviewIntent.ReportProfile -> currentState.profile?.let { openReport(it.userId, it.userId, user = true) }
            is ProfileReviewIntent.SendReport -> sendReport(intent)
            is ProfileReviewIntent.ConfirmBlock -> confirmBlock(intent.profile)
            ProfileReviewIntent.ToggleBlock -> toggleBlock()
            ProfileReviewIntent.DismissAction ->
                if (!currentState.isActing) {
                    reduce {
                        copy(
                            reportTargetId = null,
                            blockTarget = null,
                            deleteTarget = null,
                            actionFailed = false,
                            actionSucceeded = false,
                        )
                    }
                }
        }
    }

    private fun openTarget(target: ProfileReviewTarget) {
        if (this.target == target) return
        cancelTask(FEED_TASK)
        cancelTask(ACTION_TASK)
        cancelTask(LIKE_TASK)
        cancelTask(DELETE_TASK)
        this.target = target
        reduce { ProfileReviewUiState(isAuthenticated = loginRepository.hasSession()) }
        load(reset = true)
    }

    private fun load(
        reset: Boolean,
        preserveVisible: Boolean = false,
    ) {
        val target = target ?: return
        if (!reset && (!currentState.hasMore || currentState.isLoading)) return
        val visiblePageCount =
            if (preserveVisible) {
                ((currentState.reviews.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
            } else {
                1
            }
        launchTask(
            taskKey = FEED_TASK,
            loadKey = ProfileReviewLoadKey.Feed,
            policy = if (reset) TaskPolicy.CancelPrevious else TaskPolicy.IgnoreNew,
            onStart = { copy(loadFailed = false, reviews = if (reset && !preserveVisible) emptyList() else reviews) },
        ) {
            if (reset && !loadIdentity(target)) return@launchTask
            if (!currentCoroutineContext().isActive || currentState.isBlocked) return@launchTask
            val offset = if (reset) 0L else currentState.reviews.size.toLong()
            val result = fetchVisibleReviews(target.userId, offset, visiblePageCount)
            if (!currentCoroutineContext().isActive || loginRepository.hasSession() != currentState.isAuthenticated) return@launchTask
            when (result) {
                is RamapResult.Success ->
                    reduce {
                        val merged = if (reset) result.data else (reviews + result.data).distinctBy { it.id }
                        copy(reviews = merged, hasMore = result.data.size == visiblePageCount * PAGE_SIZE)
                    }

                is RamapResult.Error -> reduce { copy(loadFailed = true) }
            }
        }
    }

    private suspend fun fetchVisibleReviews(
        userId: String,
        offset: Long,
        pageCount: Int,
    ): RamapResult<List<Review>> {
        if (currentState.profile == null) return RamapResult.Success(emptyList())
        val collected = mutableListOf<Review>()
        repeat(pageCount) { page ->
            when (val result = reviewRepository.fetchProfileReviews(userId, offset + page * PAGE_SIZE)) {
                is RamapResult.Error -> return result
                is RamapResult.Success -> {
                    collected += result.data
                    if (result.data.size < PAGE_SIZE) return RamapResult.Success(collected)
                }
            }
        }
        return RamapResult.Success(collected)
    }

    private suspend fun loadIdentity(target: ProfileReviewTarget): Boolean {
        if (!loginRepository.hasSession()) {
            val profile =
                when (val result = communityRepository.fetchPublicProfile(target.userId)) {
                    is RamapResult.Success -> result.data
                    is RamapResult.Error -> {
                        if (coroutineContext.isActive) reduce { copy(loadFailed = true) }
                        return false
                    }
                }
            if (coroutineContext.isActive) {
                reduce {
                    copy(
                        isAuthenticated = false,
                        profile = profile,
                        hasMore = profile != null,
                    )
                }
            }
            return true
        }
        val ownProfileResult = communityRepository.fetchMyCommunityProfile()
        if (!coroutineContext.isActive) return false
        val ownProfile =
            when (ownProfileResult) {
                is RamapResult.Success -> ownProfileResult.data
                is RamapResult.Error -> {
                    reduce { copy(loadFailed = true) }
                    return false
                }
            }
        val blocked = communityRepository.fetchBlockedUsers()
        if (!coroutineContext.isActive) return false
        val blockedUsers =
            when (blocked) {
                is RamapResult.Success -> blocked.data
                is RamapResult.Error -> {
                    reduce { copy(loadFailed = true) }
                    return false
                }
            }
        val profile =
            if (target.userId == ownProfile?.userId) {
                ownProfile
            } else {
                when (val result = communityRepository.fetchPublicProfile(target.userId)) {
                    is RamapResult.Success -> result.data ?: blockedUsers.firstOrNull { it.userId == target.userId }
                    is RamapResult.Error -> {
                        reduce { copy(loadFailed = true) }
                        return false
                    }
                }
            }
        if (!coroutineContext.isActive) return false
        reduce {
            copy(
                isAuthenticated = true,
                currentUserId = ownProfile?.userId,
                currentProfileIsPublic =
                    currentProfileIsPublic.takeIf { currentUserId == ownProfile?.userId },
                profile = profile,
                blockedUsers = blockedUsers,
                isBlocked = profile != null && blockedUsers.any { it.userId == profile.userId },
                hasMore = profile != null,
            )
        }
        refreshCurrentProfileVisibility()
        return true
    }

    private fun refreshCurrentProfileVisibility() {
        profileVisibilityJob?.cancel()
        val userId = currentState.currentUserId ?: return
        profileVisibilityJob =
            viewModelScope.launch {
                val result = profileRepository.fetchMyProfile()
                if (currentState.currentUserId != userId) return@launch
                val profile = (result as? RamapResult.Success)?.data
                val verifiedProfile = profile?.takeIf { it.userId == userId } ?: return@launch
                reduce { copy(currentProfileIsPublic = verifiedProfile.isPublic) }
            }
    }

    private fun openReport(
        id: String,
        authorId: String,
        user: Boolean,
    ) {
        if (
            !requireLogin() ||
            currentState.currentUserId == null ||
            id.isBlank() ||
            authorId.isBlank() ||
            authorId == currentState.currentUserId
        ) {
            return
        }
        reduce { copy(reportTargetId = id, reportingUser = user, actionFailed = false, actionSucceeded = false) }
    }

    private fun toggleLike(review: Review) {
        if (review.author.userId == currentState.currentUserId ||
            !review.isPublic ||
            review.moderationStatus != ReviewModerationStatus.PUBLISHED
        ) {
            return
        }
        if (!requireLogin()) return

        val newIsLiked = !review.isLiked
        val newLikeCount = if (newIsLiked) review.likeCount + 1 else review.likeCount - 1

        launchOptimisticTask(
            taskKey = "$LIKE_TASK:${review.id}",
            isUpdating = { review.id == it.likingReviewId },
            loadKey = ProfileReviewLoadKey.Like,
            onStart = {
                copy(
                    likingReviewId = review.id,
                    reviews =
                        reviews.map { item ->
                            if (item.id == review.id) {
                                item.copy(isLiked = newIsLiked, likeCount = newLikeCount)
                            } else {
                                item
                            }
                        },
                )
            },
            onFinish = { copy(likingReviewId = null) },
            rollback = {
                copy(
                    reviews =
                        reviews.map { item ->
                            if (item.id == review.id) {
                                item.copy(isLiked = review.isLiked, likeCount = review.likeCount)
                            } else {
                                item
                            }
                        },
                )
            },
            request = { reviewRepository.setReviewLike(review.id, newIsLiked) },
            onSuccess = { result ->
                reduce {
                    copy(
                        reviews =
                            reviews.map { item ->
                                if (item.id == review.id) {
                                    item.copy(likeCount = result.likeCount, isLiked = result.isLiked)
                                } else {
                                    item
                                }
                            },
                        likingReviewId = null,
                    )
                }
            },
            onError = { reduce { copy(likingReviewId = null, actionFailed = true) } },
        )
    }

    private fun confirmDelete(review: Review) {
        if (review.author.userId != currentState.currentUserId) return
        reduce { copy(deleteTarget = review, actionFailed = false) }
    }

    private fun deleteReview() {
        val review = currentState.deleteTarget ?: return
        if (review.author.userId != currentState.currentUserId) return
        launchResultTask(
            taskKey = DELETE_TASK,
            loadKey = ProfileReviewLoadKey.Delete,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actionFailed = false) },
            request = { reviewRepository.deleteReview(review.id) },
            onSuccess = {
                reduce { copy(deleteTarget = null, reviews = reviews.filterNot { item -> item.id == review.id }) }
                load(reset = true)
            },
            onError = { reduce { copy(actionFailed = true) } },
        )
    }

    private fun confirmBlock(profile: PublicProfile) {
        if (
            !requireLogin() ||
            profile.userId.isBlank() ||
            profile.userId == currentState.currentUserId
        ) {
            return
        }
        reduce { copy(blockTarget = profile, actionFailed = false, actionSucceeded = false) }
    }

    private fun requireLogin(): Boolean {
        if (loginRepository.hasSession()) return true

        trySideEffect(ProfileReviewSideEffect.LoginRequired)
        return false
    }

    private fun sendReport(intent: ProfileReviewIntent.SendReport) {
        val id = currentState.reportTargetId ?: return
        if (!intent.reason.isValidDetails(intent.details)) return
        val reportingUser = currentState.reportingUser
        launchResultTask(
            taskKey = ACTION_TASK,
            loadKey = ProfileReviewLoadKey.Action,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actionFailed = false) },
            request = {
                if (reportingUser) {
                    communityRepository.reportUser(id, intent.reason, intent.details)
                } else {
                    communityRepository.reportReview(id, intent.reason, intent.details)
                }
            },
            onSuccess = {
                if (coroutineContext.isActive) {
                    reduce { copy(reportTargetId = null) }
                    trySideEffect(
                        ProfileReviewSideEffect.ShowToast(
                            ToastData(Res.string.review_action_success, ToastType.SUCCESS),
                        ),
                    )
                }
            },
            onError = {
                if (coroutineContext.isActive) {
                    trySideEffect(
                        ProfileReviewSideEffect.ShowToast(
                            ToastData(Res.string.review_action_failed, ToastType.ERROR),
                        ),
                    )
                }
            },
        )
    }

    private fun toggleBlock() {
        val profile = currentState.blockTarget ?: return
        if (!requireLogin() || profile.userId == currentState.currentUserId) return
        val unblock = currentState.blockedUsers.any { it.userId == profile.userId }
        launchResultTask(
            taskKey = ACTION_TASK,
            loadKey = ProfileReviewLoadKey.Action,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actionFailed = false) },
            request = { if (unblock) communityRepository.unblockUser(profile.userId) else communityRepository.blockUser(profile.userId) },
            onSuccess = {
                if (coroutineContext.isActive) {
                    reduce { copy(blockTarget = null, actionSucceeded = true) }
                    load(reset = true)
                }
            },
            onError = {
                if (coroutineContext.isActive) {
                    reduce { copy(actionFailed = true) }
                }
            },
        )
    }

    private companion object {
        const val FEED_TASK = "profile-review-feed"
        const val ACTION_TASK = "profile-review-action"
        const val LIKE_TASK = "profile-review-like"
        const val DELETE_TASK = "profile-review-delete"
        const val PAGE_SIZE = 20
    }
}
