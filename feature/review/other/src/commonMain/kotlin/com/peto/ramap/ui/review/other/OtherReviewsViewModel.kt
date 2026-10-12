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
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
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
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_action_failed

class OtherReviewsViewModel(
    private val community: CommunityRepository,
    private val profiles: ProfileRepository,
    private val follows: FollowRepository,
) : BaseViewModel<OtherReviewsUiState, OtherReviewsIntent, OtherReviewsEffect>(OtherReviewsUiState()) {
    init {
        observeSessionUserIds()
    }

    private fun observeSessionUserIds() {
        viewModelScope.launch {
            profiles
                .sessionUserIds
                .distinctUntilChanged()
                .collect { currentUserId ->
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
                    if (targetId != null) loadPage(reset = true)
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
        }
    }

    private fun handleLoadMore() {
        if (currentState.selectedTab == OtherReviewsTab.SavedShops) {
            if (currentState.savedShopsHasMore && !currentState.savedShopsLoading && !currentState.savedShopsFailed) {
                loadSavedShops()
            }
            return
        }
        if (currentState.hasMore && !currentState.loading && !currentState.failed) {
            loadPage(reset = false)
        }
    }

    private fun handleRetry() {
        if (currentState.selectedTab == OtherReviewsTab.SavedShops && currentState.profileAccess is ProfileAccess.Visible) {
            loadSavedShops()
            return
        }
        loadPage(reset = currentState.reviews.isEmpty())
    }

    private fun openProfile(userId: String) {
        if (userId.isBlank() || currentState.userId == userId) return
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
        if (currentState.sessionResolved) loadPage(reset = true)
    }

    private fun loadPage(reset: Boolean) {
        val userId = currentState.userId ?: return
        if (!currentState.sessionResolved || (!reset && currentState.profileAccess !is ProfileAccess.Visible)) return
        if (reset) {
            cancelTask(SAVED_SHOPS_TASK)
            cancelTask(FOLLOW_ACCESS_TASK)
        }
        val offset = if (reset) 0L else currentState.reviewsOffset
        launchTask(
            taskKey = PAGE_TASK,
            loadKey = OtherReviewsLoadKey.Page,
            policy = if (reset) TaskPolicy.CancelPrevious else TaskPolicy.IgnoreNew,
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
            if (reset) loadInitialPage(userId) else loadNextPage(userId, offset)
        }
    }

    private suspend fun loadInitialPage(userId: String) {
        val result = community.fetchProfileAccess(userId)
        currentCoroutineContext().ensureActive()
        when (result) {
            is RamapResult.Success ->
                when (val access = result.data) {
                    is ProfileAccess.Visible -> {
                        reduce { copy(profileAccess = access) }
                        if (currentState.currentUserId != null) loadSavedShops()
                        if (access.canReadReviews) fetchAndApplyUserReviews(userId, 0L, reset = true)
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
        if (currentState.userId != userId) return
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
        val result = community.fetchProfileReviewsPage(userId, offset)
        currentCoroutineContext().ensureActive()
        when (result) {
            is RamapResult.Success -> {
                if (currentState.userId != userId) return
                val page = result.data
                val access = page.access as? ProfileAccess.Visible
                if (access == null) {
                    cancelTask(SAVED_SHOPS_TASK)
                    applyProfileAccess(userId, page.access)
                    return
                }
                if (!access.canReadReviews || !access.canReadSavedShops) cancelTask(SAVED_SHOPS_TASK)
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
        if (currentState.userId == userId) reduce { copy(failed = true) }
    }

    private fun toggleFollow() {
        val access = currentState.profileAccess as? ProfileAccess.Visible ?: return
        val currentUserId = currentState.currentUserId
        if (currentUserId == null) {
            trySideEffect(OtherReviewsEffect.LoginRequired)
            return
        }
        if (access.profile.userId == currentUserId || currentState.following) return
        val action = if (access.followState == FollowState.NONE) FollowAction.FOLLOW else FollowAction.UNFOLLOW
        launchResultTask(
            taskKey = FOLLOW_TASK,
            loadKey = OtherReviewsLoadKey.Follow,
            policy = TaskPolicy.IgnoreNew,
            request = { follows.changeFollow(access.profile.userId, action) },
            onSuccess = {
                if (currentState.currentUserId == currentUserId && currentState.userId == access.profile.userId) {
                    refreshFollowAccess(access.profile.userId)
                }
            },
            onError = { handleToggleBlockError(access.profile.userId) },
        )
    }

    private fun toggleBlock() {
        val profile = currentState.profile ?: return
        val currentUserId = currentState.currentUserId
        if (currentUserId == null) {
            trySideEffect(OtherReviewsEffect.LoginRequired)
            return
        }
        if (profile.userId == currentUserId || currentState.blocking) return
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
                handleToggleBlockError(profile.userId)
            },
        )
    }

    private fun refreshFollowAccess(userId: String) {
        val previousAccess = currentState.profileAccess as? ProfileAccess.Visible ?: return
        val currentUserId = currentState.currentUserId
        val resumeReviews = currentState.loading
        val resumeSavedShops = currentState.savedShopsLoading
        // Discard in-flight pages carrying the old follow state or permissions.
        cancelTask(PAGE_TASK)
        cancelTask(SAVED_SHOPS_TASK)
        launchResultTask(
            taskKey = FOLLOW_ACCESS_TASK,
            loadKey = OtherReviewsLoadKey.Follow,
            policy = TaskPolicy.CancelPrevious,
            request = { community.fetchProfileAccess(userId) },
            onSuccess = { access ->
                if (currentState.currentUserId != currentUserId || currentState.userId != userId) return@launchResultTask
                applyFollowAccess(userId, access)
                if (access !is ProfileAccess.Visible) return@launchResultTask
                if (access.canReadReviews &&
                    (
                        !previousAccess.canReadReviews ||
                            resumeReviews ||
                            (currentState.reviewsOffset == 0L && !currentState.failed && access.reviewCount != 0L)
                    )
                ) {
                    loadPage(reset = false)
                }
                if (access.canReadSavedShops &&
                    (
                        !previousAccess.canReadSavedShops ||
                            resumeSavedShops ||
                            (!currentState.savedShopsLoaded && !currentState.savedShopsFailed)
                    )
                ) {
                    loadSavedShops()
                }
            },
            onError = { handleToggleBlockError(userId) },
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
        if (currentState.userId != targetProfile.userId || currentState.currentUserId != currentUserId) return
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

    private suspend fun handleToggleBlockError(targetUserId: String) {
        if (currentState.userId == targetUserId) {
            postSideEffect(
                OtherReviewsEffect.ShowToast(
                    ToastData(
                        message = Res.string.review_action_failed,
                        type = ToastType.ERROR,
                    ),
                ),
            )
        }
    }

    private fun selectTab(tab: OtherReviewsTab) {
        if (tab == OtherReviewsTab.SavedShops && currentState.currentUserId == null) {
            trySideEffect(OtherReviewsEffect.LoginRequired)
            return
        }
        reduce { copy(selectedTab = tab) }
        if (tab == OtherReviewsTab.SavedShops && !currentState.savedShopsLoaded) loadSavedShops()
    }

    private fun loadSavedShops() {
        val userId = currentState.userId ?: return
        if (currentState.currentUserId == null || (currentState.profileAccess as? ProfileAccess.Visible)?.canReadSavedShops != true) return
        val offset = currentState.savedShopsOffset
        launchResultTask(
            taskKey = SAVED_SHOPS_TASK,
            loadKey = OtherReviewsLoadKey.SavedShops,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(savedShopsFailed = false) },
            request = { community.fetchUserSavedShops(userId, offset) },
            onSuccess = { page ->
                currentCoroutineContext().ensureActive()
                if (currentState.userId == userId) applySavedShopsPage(page)
            },
            onError = {
                currentCoroutineContext().ensureActive()
                if (currentState.userId == userId) reduce { copy(savedShopsFailed = true) }
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
        if (!access.canReadReviews || !access.canReadSavedShops) cancelTask(PAGE_TASK)
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
