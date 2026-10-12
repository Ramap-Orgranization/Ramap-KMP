package com.peto.ramap.ui.profile.follow

import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowCounts
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class FollowState(
    val currentUserId: String? = null,
    val selectedList: FollowList = FollowList.FOLLOWING,
    val counts: FollowCounts? = null,
    val countsFailed: Boolean = false,
    val actingUserId: String? = null,
    val actingAction: FollowAction? = null,
    val profiles: List<PublicProfile> = emptyList(),
    val offset: Long = 0L,
    val hasMore: Boolean = false,
    val failed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<FollowState> {
    val loading: Boolean get() = loadState.isLoading(FollowLoadKey.Page)
    val mutating: Boolean get() = loadState.isLoading(FollowLoadKey.Mutation)
    val availableLists: List<FollowList>
        get() = listOf(FollowList.FOLLOWING, FollowList.FOLLOWERS) + if ((counts?.requests ?: 0L) > 0L) listOf(FollowList.REQUESTS) else emptyList()

    override fun withLoadingState(loadState: LoadState): FollowState = copy(loadState = loadState)
}
