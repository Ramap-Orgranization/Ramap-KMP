package com.peto.ramap.ui.review.other

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowState
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsLoadKey
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
import com.peto.ramap.ui.review.other.contract.OtherReviewsUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_load_failure_message
import ramap.shared.generated.resources.follow_update_failure_message
import ramap.shared.generated.resources.login_required_block_description
import ramap.shared.generated.resources.login_required_follow_description
import ramap.shared.generated.resources.login_required_review_like_description
import ramap.shared.generated.resources.login_required_saved_shops_description
import ramap.shared.generated.resources.review_block_failed
import ramap.shared.generated.resources.review_like_failed
import ramap.shared.generated.resources.review_unblock_failed

import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ShopReviewsPage

class OtherReviewsViewModel(
    private val community: CommunityRepository,
    private val profiles: ProfileRepository,
    private val follows: FollowRepository,
    private val reviewRepository: ReviewRepository =
        object : ReviewRepository {
            override suspend fun fetchMyReviews(offset: Long, visibility: MyReviewVisibility) = RamapResult.Success(MyReviewsPage(emptyList(), 0, 0, 0, true))
            override suspend fun fetchMyReview(reviewId: String) = RamapResult.Success(null)
            override suspend fun fetchShopReviewsPage(shopId: String, offset: Long) = RamapResult.Success(ShopReviewsPage(emptyList(), 0))
            override suspend fun fetchBlockedShopReviewOnce(reviewId: String) = RamapResult.Success(null)
            override suspend fun submitReview(shopId: String, body: String, images: List<ReviewImage>, isPublic: Boolean) = RamapResult.Success(Unit)
            override suspend fun fetchEditableReview(reviewId: String) = RamapResult.Success(null)
            override suspend fun updateReview(reviewId: String, body: String, retainedImagePaths: List<String>, newImages: List<ReviewImage>, isPublic: Boolean) = RamapResult.Success(Unit)
            override suspend fun deleteReview(reviewId: String) = RamapResult.Success(Unit)
            override suspend fun setReviewLike(reviewId: String, liked: Boolean) = RamapResult.Success(ReviewLike(0, liked))
        },
    private val changes: ReviewChangeStore,
    private val reviewChangeReducer: ReviewChangeReducer,
) : BaseViewModel<OtherReviewsUiState, OtherReviewsIntent, OtherReviewsEffect>(OtherReviewsUiState()) {
    private var generation = 0L

    init {
        observeChanges()
        observeSessionUserIds()
    }

    private fun observeChanges() {
        viewModelScope.launch {
            var epoch = changes.journal.value.epoch
            var revision = changes.journal.value.revision
            changes.journal.collect { journal ->
                val isEpochChanged = epoch != journal.epoch
                if (isEpochChanged) {
                    epoch = journal.epoch
                    revision = journal.revision
                    return@collect
                }
                val hasGap = journal.hasGapAfter(revision)
                if (hasGap) {
                    revision = journal.revision
                    loadPage(reset = true)
                    return@collect
                }
                for (record in journal.changesAfter(revision)) {
                    val change = record.change
                    val targetId = currentState.userId
                    val isAccessChangedForUser = change is ReviewChange.AccessChanged && change.userId == targetId
                    if (isAccessChangedForUser) {
                        handleAccessChange(change as ReviewChange.AccessChanged)
                    } else {
                        reduce {
                            val access = profileAccess as? ProfileAccess.Visible ?: return@reduce this
                            val cannotReadReviews = !access.canReadReviews
                            if (cannotReadReviews) return@reduce this
                            val patched =
                                reviewChangeReducer.updateProfileRows(
                                    rows = reviews,
                                    change = change,
                                    hasMore = hasMore,
                                    accepts = { receipt, review ->
                                        val isTargetUser = receipt.userId == targetId
                                        val isAccessible = targetId == currentUserId || reviewChangeReducer.isPublicPublished(review)
                                        isTargetUser && isAccessible
                                    },
                                )
                            val ownerCounts = (change as? ReviewChange.Written)?.takeIf { it.userId == targetId }?.counts
                            val isCurrentSelf = targetId == currentUserId
                            val count = ownerCounts?.let { if (isCurrentSelf) it.totalCount else it.publishedCount }
                            copy(
                                reviews = patched,
                                reviewsOffset = patched.size.toLong(),
                                profileAccess = access.copy(reviewCount = count?.toLong() ?: access.reviewCount),
                                hasMore = count?.let { patched.size < it } ?: hasMore,
                            )
                        }
                    }
                    revision = record.revision
                }
            }
        }
    }

    private fun handleAccessChange(change: ReviewChange.AccessChanged) {
        val canRefreshFollowAccess = change.followAction != null && currentState.profileAccess is ProfileAccess.Visible
        if (canRefreshFollowAccess) {
            // 현재 화면의 팔로우 작업은 성공 콜백에서 접근 상태를 갱신한다.
            if (!currentState.following) refreshFollowAccess(change.userId)
            return
        }
        generation++
        val extent = currentState.reviews.size
        reduce { copy(reviews = emptyList(), profileAccess = null, hasMore = false) }
        loadPage(reset = true, requestedReviewExtent = extent)
    }

    private fun observeSessionUserIds() {
        viewModelScope.launch {
            profiles
                .sessionUserId
                .distinctUntilChanged()
                .collect { currentUserId ->
                    generation++
                    val targetId = currentState.userId
                    cancelTask(PAGE_TASK)
                    cancelTask(SAVED_SHOPS_TASK)
                    cancelTask(BLOCK_TASK)
                    cancelTask(FOLLOW_TASK)
                    cancelTask(FOLLOW_ACCESS_TASK)
                    reduce {
                        OtherReviewsUiState(
                            userId = targetId,
                            currentUserId = currentUserId,
                            sessionResolved = true,
                        )
                    }
                    val hasTargetUser = targetId != null
                    if (hasTargetUser) loadPage(reset = true)
                }
        }
    }

    override suspend fun handleIntent(intent: OtherReviewsIntent) {
        when (intent) {
            is OtherReviewsIntent.SelectTab -> selectTab(intent.tab)
            is OtherReviewsIntent.OpenProfile -> openProfile(intent.userId)
            OtherReviewsIntent.LoadMore -> handleLoadMore()
            OtherReviewsIntent.Retry -> handleRetry()
            OtherReviewsIntent.ToggleBlock -> toggleBlock()
            OtherReviewsIntent.ToggleFollow -> toggleFollow()
            is OtherReviewsIntent.ToggleLike -> toggleLike(intent.review)
        }
    }

    private fun toggleLike(review: Review) {
        val currentUserId = currentState.currentUserId
        val isLoggedOut = currentUserId == null
        if (isLoggedOut) {
            trySideEffect(OtherReviewsEffect.LoginRequired(Res.string.login_required_review_like_description))
            return
        }
        val targetId = currentState.userId ?: return
        val next = review.toggledLike()
        val originalReviewLike = ReviewLike(review.likeCount, review.isLiked)
        launchOptimisticTask(
            taskKey = "other-review-like-${review.id}",
            loadKey = OtherReviewsLoadKey.ReviewAction,
            isUpdating = { false },
            onStart = {
                val patched =
                    reviewChangeReducer.updateProfileRows(
                        rows = reviews,
                        change = ReviewChange.Liked(review.id, next),
                        hasMore = hasMore,
                        accepts = { receipt, _ -> receipt.userId == targetId },
                    )
                copy(reviews = patched)
            },
            rollback = {
                val patched =
                    reviewChangeReducer.updateProfileRows(
                        rows = reviews,
                        change = ReviewChange.Liked(review.id, originalReviewLike),
                        hasMore = hasMore,
                        accepts = { receipt, _ -> receipt.userId == targetId },
                    )
                copy(reviews = patched)
            },
            request = { reviewRepository.setReviewLike(review.id, next.isLiked) },
            onError = {
                postSideEffect(
                    OtherReviewsEffect.ShowToast(
                        ToastData(
                            message = Res.string.review_like_failed,
                            type = ToastType.ERROR,
                        ),
                    ),
                )
            },
        )
    }

    private fun handleLoadMore() {
        val isSavedShopsTab = currentState.selectedTab == OtherReviewsTab.SavedShops
        if (isSavedShopsTab) {
            val canLoadMoreSavedShops = currentState.savedShopsHasMore && !currentState.savedShopsLoading && !currentState.savedShopsFailed
            if (canLoadMoreSavedShops) {
                loadSavedShops()
            }
            return
        }
        val canLoadMoreReviews = currentState.hasMore && !currentState.loading && !currentState.failed
        if (canLoadMoreReviews) {
            loadPage(reset = false)
        }
    }

    private fun handleRetry() {
        val isSavedShopsTab = currentState.selectedTab == OtherReviewsTab.SavedShops
        val isProfileVisible = currentState.profileAccess is ProfileAccess.Visible
        if (isSavedShopsTab && isProfileVisible) {
            loadSavedShops()
            return
        }
        val isReviewsEmpty = currentState.reviews.isEmpty()
        loadPage(reset = isReviewsEmpty)
    }

    private fun openProfile(userId: String) {
        val isInvalidOrSameUser = userId.isBlank() || currentState.userId == userId
        if (isInvalidOrSameUser) return
        generation++
        cancelTask(PAGE_TASK)
        cancelTask(SAVED_SHOPS_TASK)
        cancelTask(BLOCK_TASK)
        cancelTask(FOLLOW_TASK)
        cancelTask(FOLLOW_ACCESS_TASK)
        reduce {
            OtherReviewsUiState(
                userId = userId,
                currentUserId = currentUserId,
                sessionResolved = sessionResolved,
            )
        }
        val isSessionResolved = currentState.sessionResolved
        if (isSessionResolved) loadPage(reset = true)
    }

    private fun loadPage(
        reset: Boolean,
        requestedReviewExtent: Int = if (reset) currentState.reviews.size else 0,
    ) {
        val userId = currentState.userId ?: return
        val isNotResolved = !currentState.sessionResolved
        val isNotVisibleOnNextPage = !reset && currentState.profileAccess !is ProfileAccess.Visible
        if (isNotResolved || isNotVisibleOnNextPage) return
        if (reset) {
            cancelTask(SAVED_SHOPS_TASK)
            cancelTask(FOLLOW_ACCESS_TASK)
        }
        val offset = if (reset) 0L else currentState.reviewsOffset
        launchTask(
            taskKey = PAGE_TASK,
            loadKey = OtherReviewsLoadKey.Page,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                if (reset) {
                    copy(
                        profileAccess = null,
                        reviews = emptyList(),
                        reviewsOffset = 0L,
                        failed = false,
                        hasMore = false,
                        savedShops = emptyList(),
                        savedShopsLoaded = false,
                        savedShopsFailed = false,
                        savedShopsHasMore = false,
                        savedShopsOffset = 0L,
                    )
                } else {
                    copy(failed = false)
                }
            },
        ) {
            if (reset) loadInitialPage(userId, requestedReviewExtent) else loadNextPage(userId, offset)
        }
    }

    private suspend fun loadInitialPage(
        userId: String,
        requestedReviewExtent: Int,
    ) {
        val requestedGeneration = generation
        val result = community.fetchProfileAccess(userId)
        currentCoroutineContext().ensureActive()
        val isStaleGeneration = requestedGeneration != generation
        if (isStaleGeneration) return
        when (result) {
            is RamapResult.Success ->
                when (val access = result.data) {
                    is ProfileAccess.Visible -> {
                        reduce { copy(profileAccess = access) }
                        val hasCurrentUser = currentState.currentUserId != null
                        if (hasCurrentUser) loadSavedShops()
                        if (access.canReadReviews) {
                            fetchAndApplyUserReviews(userId, 0L, reset = true)
                            while (currentState.reviews.size < requestedReviewExtent && currentState.hasMore && !currentState.failed) {
                                val offset = currentState.reviewsOffset
                                fetchAndApplyUserReviews(userId, offset, reset = false)
                                val isOffsetStuck = currentState.reviewsOffset == offset
                                if (isOffsetStuck) break
                            }
                        }
                    }
                    else -> applyProfileAccess(userId, access)
                }
            is RamapResult.Error -> fail(userId)
        }
    }

    private suspend fun loadNextPage(
        userId: String,
        offset: Long,
    ) {
        val access = currentState.profileAccess as? ProfileAccess.Visible ?: return
        if (access.canReadReviews) fetchAndApplyUserReviews(userId, offset, reset = false)
    }

    private fun applyProfileAccess(
        userId: String,
        access: ProfileAccess,
    ) {
        val isDifferentUser = currentState.userId != userId
        if (isDifferentUser) return
        reduce {
            copy(
                profileAccess = access,
                reviews = emptyList(),
                reviewsOffset = 0L,
                hasMore = false,
                failed = false,
                savedShops = emptyList(),
                savedShopsLoaded = false,
                savedShopsFailed = false,
                savedShopsHasMore = false,
                savedShopsOffset = 0L,
            )
        }
    }

    private suspend fun fetchAndApplyUserReviews(
        userId: String,
        offset: Long,
        reset: Boolean,
    ) {
        val requestedGeneration = generation
        val baseline = changes.journal.value
        val result = community.fetchProfileReviewsPage(userId, offset)
        currentCoroutineContext().ensureActive()
        val isStaleGeneration = requestedGeneration != generation
        if (isStaleGeneration) return
        val isEpochChanged = baseline.epoch != changes.journal.value.epoch
        if (isEpochChanged) {
            loadPage(reset = true)
            return
        }
        val hasGap = changes.journal.value.hasGapAfter(baseline.revision)
        val hasUserChanges =
            changes.journal.value
                .changesAfter(baseline.revision)
                .any { (it.change as? ReviewChange.Written)?.userId == userId || (it.change as? ReviewChange.AccessChanged)?.userId == userId }
        if (hasGap || hasUserChanges) {
            loadPage(reset)
            return
        }
        when (result) {
            is RamapResult.Success -> {
                val isDifferentUser = currentState.userId != userId
                if (isDifferentUser) return
                val original = result.data
                var rows = original.reviews
                for (record in changes.journal.value.changesAfter(baseline.revision)) {
                    rows =
                        reviewChangeReducer.updateProfileRows(
                            rows = rows,
                            change = record.change,
                            hasMore = original.reviews.size == PAGE_SIZE,
                            accepts = { receipt, review ->
                                val isUserReceipt = receipt.userId == userId
                                val isAccessible = userId == currentState.currentUserId || reviewChangeReducer.isPublicPublished(review)
                                isUserReceipt && isAccessible
                            },
                        )
                }
                val page = original.copy(reviews = rows)
                val access = page.access as? ProfileAccess.Visible
                if (access == null) {
                    cancelTask(SAVED_SHOPS_TASK)
                    applyProfileAccess(userId, page.access)
                    return
                }
                val cannotReadShopsOrReviews = !access.canReadReviews || !access.canReadSavedShops
                if (cannotReadShopsOrReviews) cancelTask(SAVED_SHOPS_TASK)
                reduce {
                    copy(
                        profileAccess = access.copy(profile = profile ?: access.profile),
                        reviews =
                            if (access.canReadReviews) {
                                (if (reset) page.reviews else reviews + page.reviews).distinctBy { it.review.id }
                            } else {
                                emptyList()
                            },
                        hasMore = access.canReadReviews && page.reviews.size == PAGE_SIZE,
                        reviewsOffset = if (access.canReadReviews) offset + page.reviews.size else 0L,
                        savedShops = if (access.canReadSavedShops) savedShops else emptyList(),
                        savedShopsHasMore = access.canReadSavedShops && savedShopsHasMore,
                    )
                }
            }

            is RamapResult.Error -> fail(userId)
        }
    }

    private fun fail(userId: String) {
        val isSameUser = currentState.userId == userId
        if (isSameUser) reduce { copy(failed = true) }
    }

    private fun toggleFollow() {
        val access = currentState.profileAccess as? ProfileAccess.Visible ?: return
        val currentUserId = currentState.currentUserId
        val isLoggedOut = currentUserId == null
        if (isLoggedOut) {
            trySideEffect(OtherReviewsEffect.LoginRequired(Res.string.login_required_follow_description))
            return
        }
        val isSelfOrMutating = access.profile.userId == currentUserId || currentState.following
        if (isSelfOrMutating) return
        val action = if (access.followState == FollowState.NONE) FollowAction.FOLLOW else FollowAction.UNFOLLOW
        launchResultTask(
            taskKey = FOLLOW_TASK,
            loadKey = OtherReviewsLoadKey.Follow,
            policy = TaskPolicy.IgnoreNew,
            request = { follows.changeFollow(access.profile.userId, action) },
            onSuccess = {
                val isCurrentContextValid = currentState.currentUserId == currentUserId && currentState.userId == access.profile.userId
                if (isCurrentContextValid) {
                    refreshFollowAccess(access.profile.userId)
                }
            },
            onError = {
                showActionFailure(
                    targetUserId = access.profile.userId,
                    message = Res.string.follow_update_failure_message,
                )
            },
        )
    }

    private fun toggleBlock() {
        val profile = currentState.profile ?: return
        val currentUserId = currentState.currentUserId
        val isLoggedOut = currentUserId == null
        if (isLoggedOut) {
            trySideEffect(OtherReviewsEffect.LoginRequired(Res.string.login_required_block_description))
            return
        }
        val isSelfOrBlocking = profile.userId == currentUserId || currentState.blocking
        if (isSelfOrBlocking) return
        val wasBlocked = currentState.isBlocked
        launchResultTask(
            taskKey = BLOCK_TASK,
            loadKey = OtherReviewsLoadKey.Block,
            policy = TaskPolicy.IgnoreNew,
            request = {
                if (wasBlocked) community.unblockUser(profile.userId) else community.blockUser(profile.userId)
            },
            onSuccess = {
                handleToggleBlockSuccess(
                    targetProfile = profile,
                    currentUserId = currentUserId,
                    wasBlocked = wasBlocked,
                )
            },
            onError = {
                showActionFailure(
                    targetUserId = profile.userId,
                    message = if (wasBlocked) Res.string.review_unblock_failed else Res.string.review_block_failed,
                )
            },
        )
    }

    private fun refreshFollowAccess(userId: String) {
        val previousAccess = currentState.profileAccess as? ProfileAccess.Visible ?: return
        val currentUserId = currentState.currentUserId
        val resumeReviews = currentState.loading
        val resumeSavedShops = currentState.savedShopsLoading
        cancelTask(PAGE_TASK)
        cancelTask(SAVED_SHOPS_TASK)
        launchResultTask(
            taskKey = FOLLOW_ACCESS_TASK,
            loadKey = OtherReviewsLoadKey.Follow,
            policy = TaskPolicy.CancelPrevious,
            request = { community.fetchProfileAccess(userId) },
            onSuccess = { access ->
                val isStaleContext = currentState.currentUserId != currentUserId || currentState.userId != userId
                if (isStaleContext) return@launchResultTask
                applyFollowAccess(userId, access)
                if (access !is ProfileAccess.Visible) return@launchResultTask

                val shouldLoadPage =
                    access.canReadReviews &&
                        (
                            !previousAccess.canReadReviews ||
                                resumeReviews ||
                                (currentState.reviewsOffset == 0L && !currentState.failed && access.reviewCount != 0L)
                        )
                if (shouldLoadPage) {
                    loadPage(reset = false)
                }

                val shouldLoadSavedShops =
                    access.canReadSavedShops &&
                        (
                            !previousAccess.canReadSavedShops ||
                                resumeSavedShops ||
                                (!currentState.savedShopsLoaded && !currentState.savedShopsFailed)
                        )
                if (shouldLoadSavedShops) {
                    loadSavedShops()
                }
            },
            onError = {
                showActionFailure(
                    targetUserId = userId,
                    message = Res.string.follow_load_failure_message,
                )
            },
        )
    }

    private fun applyFollowAccess(
        userId: String,
        access: ProfileAccess,
    ) {
        if (access !is ProfileAccess.Visible) {
            applyProfileAccess(userId, access)
            return
        }
        reduce {
            copy(
                profileAccess = access.copy(profile = profile ?: access.profile),
                reviews = if (access.canReadReviews) reviews else emptyList(),
                reviewsOffset = if (access.canReadReviews) reviewsOffset else 0L,
                hasMore = access.canReadReviews && hasMore,
                failed = access.canReadReviews && failed,
                savedShops = if (access.canReadSavedShops) savedShops else emptyList(),
                savedShopsOffset = if (access.canReadSavedShops) savedShopsOffset else 0L,
                savedShopsLoaded = access.canReadSavedShops && savedShopsLoaded,
                savedShopsHasMore = access.canReadSavedShops && savedShopsHasMore,
                savedShopsFailed = access.canReadSavedShops && savedShopsFailed,
            )
        }
    }

    private fun handleToggleBlockSuccess(
        targetProfile: PublicProfile,
        currentUserId: String,
        wasBlocked: Boolean,
    ) {
        val isStaleUser = currentState.userId != targetProfile.userId || currentState.currentUserId != currentUserId
        if (isStaleUser) return
        cancelTask(PAGE_TASK)
        cancelTask(SAVED_SHOPS_TASK)
        cancelTask(FOLLOW_ACCESS_TASK)
        reduce {
            copy(
                profileAccess =
                    if (wasBlocked) {
                        null
                    } else {
                        ProfileAccess.Blocked(PublicProfile(targetProfile.userId, targetProfile.nickname))
                    },
                reviews = emptyList(),
                reviewsOffset = 0L,
                hasMore = false,
                failed = false,
                savedShops = emptyList(),
                savedShopsLoaded = false,
                savedShopsFailed = false,
                savedShopsHasMore = false,
                savedShopsOffset = 0L,
            )
        }
        if (wasBlocked) loadPage(reset = true)
    }

    private suspend fun showActionFailure(
        targetUserId: String,
        message: StringResource,
    ) {
        val isSameUser = currentState.userId == targetUserId
        if (isSameUser) {
            postSideEffect(
                OtherReviewsEffect.ShowToast(
                    ToastData(
                        message = message,
                        type = ToastType.ERROR,
                    ),
                ),
            )
        }
    }

    private fun selectTab(tab: OtherReviewsTab) {
        val isSavedShopsAndLoggedOut = tab == OtherReviewsTab.SavedShops && currentState.currentUserId == null
        if (isSavedShopsAndLoggedOut) {
            trySideEffect(OtherReviewsEffect.LoginRequired(Res.string.login_required_saved_shops_description))
            return
        }
        reduce { copy(selectedTab = tab) }
        val shouldLoadSavedShops = tab == OtherReviewsTab.SavedShops && !currentState.savedShopsLoaded
        if (shouldLoadSavedShops) loadSavedShops()
    }

    private fun loadSavedShops() {
        val userId = currentState.userId ?: return
        val isLoggedOutOrCannotRead = currentState.currentUserId == null || (currentState.profileAccess as? ProfileAccess.Visible)?.canReadSavedShops != true
        if (isLoggedOutOrCannotRead) return
        val offset = currentState.savedShopsOffset
        launchResultTask(
            taskKey = SAVED_SHOPS_TASK,
            loadKey = OtherReviewsLoadKey.SavedShops,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(savedShopsFailed = false) },
            request = { community.fetchUserSavedShops(userId, offset) },
            onSuccess = { page ->
                currentCoroutineContext().ensureActive()
                val isSameUser = currentState.userId == userId
                if (isSameUser) applySavedShopsPage(page)
            },
            onError = {
                currentCoroutineContext().ensureActive()
                val isSameUser = currentState.userId == userId
                if (isSameUser) reduce { copy(savedShopsFailed = true) }
            },
        )
    }

    private fun applySavedShopsPage(page: PublicSavedShopsPage) {
        val access = page.access as? ProfileAccess.Visible
        if (access == null) {
            cancelTask(PAGE_TASK)
            applyProfileAccess(currentState.userId ?: return, page.access)
            return
        }
        val cannotReadShopsOrReviews = !access.canReadReviews || !access.canReadSavedShops
        if (cannotReadShopsOrReviews) cancelTask(PAGE_TASK)
        reduce {
            copy(
                profileAccess = access.copy(profile = profile ?: access.profile),
                reviews = if (access.canReadReviews) reviews else emptyList(),
                hasMore = access.canReadReviews && hasMore,
                savedShops = if (access.canReadSavedShops) (savedShops + page.shops).distinctBy { it.id } else emptyList(),
                savedShopsLoaded = true,
                savedShopsHasMore = access.canReadSavedShops && page.shops.size == PAGE_SIZE,
                savedShopsOffset = savedShopsOffset + page.shops.size,
            )
        }
    }

    private companion object {
        const val FOLLOW_ACCESS_TASK = "other-profile-follow-access"
        const val SAVED_SHOPS_TASK = "other-profile-saved-shops"
        const val PAGE_TASK = "other-reviews-page"
        const val FOLLOW_TASK = "other-profile-follow"
        const val BLOCK_TASK = "other-reviews-block"
        const val PAGE_SIZE = 20
    }
}
