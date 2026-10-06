package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class MyTabUiState(
    val userId: String? = null,
    val sessionResolved: Boolean = false,
    val profile: AccountProfile? = null,
    val failed: Boolean = false,
    val bookmarkedCount: Int? = null,
    val notificationCount: Int? = null,
    val hiddenCount: Int? = null,
    val reviewCount: Int? = null,
    val blockedUserCount: Int? = null,
    val hasPendingFollowRequests: Boolean = false,
    val blockedUsers: List<BlockedUser> = emptyList(),
    val pendingUnblockUser: PublicProfile? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<MyTabUiState> {
    val loading: Boolean get() = loadState.isLoading(MyTabLoadKey.Fetch)
    val savingVisibility: Boolean get() = loadState.isLoading(MyTabLoadKey.Visibility)
    val loadingReviewCount: Boolean get() = loadState.isLoading(MyTabLoadKey.ReviewCount)
    val loadingBlockedUserCount: Boolean get() = loadState.isLoading(MyTabLoadKey.BlockedUserCount)
    val unblocking: Boolean get() = loadState.isLoading(MyTabLoadKey.Unblock)

    override fun withLoadingState(loadState: LoadState): MyTabUiState = copy(loadState = loadState)
}
