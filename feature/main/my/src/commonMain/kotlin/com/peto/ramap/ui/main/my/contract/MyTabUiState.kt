package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class MyTabUiState(
    val userId: String? = null,
    val profile: AccountProfile? = null,
    val failed: Boolean = false,
    val bookmarkedCount: Int? = null,
    val notificationCount: Int? = null,
    val hiddenCount: Int? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<MyTabUiState> {
    val loading: Boolean get() = loadState.isLoading(MyTabLoadKey.Fetch)

    override fun withLoadingState(loadState: LoadState): MyTabUiState = copy(loadState = loadState)
}
