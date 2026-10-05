package com.peto.ramap.ui.review.other

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.repository.CommunityRepository
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
) : BaseViewModel<OtherReviewsUiState, OtherReviewsIntent, OtherReviewsEffect>(OtherReviewsUiState()) {
    init {
        observeSessionUserIds()
        observeCommunityChanges()
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

    private fun observeCommunityChanges() {
        viewModelScope.launch {
            community.observeChanges().collect {
                if (currentState.userId != null && currentState.sessionResolved) {
                    loadPage(reset = true)
                }
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
        if (reset) cancelTask(SAVED_SHOPS_TASK)
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
                        fetchAndApplyUserReviews(userId, access.profile, 0L, reset = true)
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
        fetchAndApplyUserReviews(userId, access.profile, offset, reset = false)
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
        profile: PublicProfile,
        offset: Long,
        reset: Boolean,
    ) {
        val result = community.fetchUserReviews(userId, offset)
        currentCoroutineContext().ensureActive()
        when (result) {
            is RamapResult.Success -> {
                if (currentState.userId != userId) return
                reduce {
                    copy(
                        profileAccess = ProfileAccess.Visible(profile),
                        reviews = (if (reset) result.data else reviews + result.data).distinctBy { it.review.id },
                        hasMore = result.data.size == PAGE_SIZE,
                        reviewsOffset = offset + result.data.size,
                    )
                }
            }

            is RamapResult.Error -> fail(userId)
        }
    }

    private fun fail(userId: String) {
        if (currentState.userId == userId) reduce { copy(failed = true) }
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

    private fun handleToggleBlockSuccess(
        targetProfile: PublicProfile,
        currentUserId: String,
        wasBlocked: Boolean,
    ) {
        if (currentState.userId != targetProfile.userId || currentState.currentUserId != currentUserId) return
        cancelTask(PAGE_TASK)
        cancelTask(SAVED_SHOPS_TASK)
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
        if (currentState.currentUserId == null || currentState.profileAccess !is ProfileAccess.Visible) return
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
        if (page.access !is ProfileAccess.Visible) {
            cancelTask(PAGE_TASK)
            applyProfileAccess(currentState.userId ?: return, page.access)
            return
        }
        reduce {
            copy(
                savedShops = (savedShops + page.shops).distinctBy { it.id },
                savedShopsLoaded = true,
                savedShopsHasMore = page.shops.size == PAGE_SIZE,
                savedShopsOffset = savedShopsOffset + page.shops.size,
            )
        }
    }

    private companion object {
        const val SAVED_SHOPS_TASK = "other-profile-saved-shops"
        const val PAGE_TASK = "other-reviews-page"
        const val BLOCK_TASK = "other-reviews-block"
        const val PAGE_SIZE = 20
    }
}
