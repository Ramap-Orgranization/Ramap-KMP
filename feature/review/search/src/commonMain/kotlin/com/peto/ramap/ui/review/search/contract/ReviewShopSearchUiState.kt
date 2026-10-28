package com.peto.ramap.ui.review.search.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ReviewShopSearchUiState(
    val query: String = "",
    val results: RamenShops = RamenShops(emptyMap()),
    val recentShops: RamenShops = RamenShops(emptyMap()),
    val hasSearched: Boolean = false,
    val hasSearchError: Boolean = false,
    val hasRecentShopsError: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ReviewShopSearchUiState> {
    val isShowingHistory: Boolean get() = query.isBlank()

    val shops: RamenShops get() = if (isShowingHistory) recentShops else results

    val isLoading: Boolean
        get() = loadState.isLoading(if (isShowingHistory) ReviewShopSearchLoadKey.RecentShops else ReviewShopSearchLoadKey.Search)

    val hasError: Boolean get() = if (isShowingHistory) hasRecentShopsError else hasSearchError

    override fun withLoadingState(loadState: LoadState): ReviewShopSearchUiState = copy(loadState = loadState)
}
