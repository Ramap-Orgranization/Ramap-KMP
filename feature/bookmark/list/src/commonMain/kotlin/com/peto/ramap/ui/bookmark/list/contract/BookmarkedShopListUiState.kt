package com.peto.ramap.ui.bookmark.list.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class BookmarkedShopListUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val showError: Boolean = false,
    val hasLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<BookmarkedShopListUiState> {
    override fun withLoadingState(loadState: LoadState): BookmarkedShopListUiState = copy(loadState = loadState)

    val isOnlyLoading: Boolean =
        shops.isEmpty() &&
            (!hasLoaded || loadState.isLoading(BookmarkedShopLoadKey.FETCH))

    val isOverlayLoading =
        loadState.isLoading(BookmarkedShopLoadKey.REMOVE) ||
            loadState.isLoading(BookmarkedShopLoadKey.FETCH)
}
