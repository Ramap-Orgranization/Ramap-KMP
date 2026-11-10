package com.peto.ramap.ui.bookmark.list.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class BookmarkedShopListUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val remainingShopCount: Int = 0,
    val showError: Boolean = false,
    val appendFailed: Boolean = false,
    val hasLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<BookmarkedShopListUiState> {
    override fun withLoadingState(loadState: LoadState): BookmarkedShopListUiState = copy(loadState = loadState)

    val hasMore: Boolean get() = remainingShopCount > 0

    val shopCount: Int get() = shops.size + remainingShopCount

    val isPageLoading: Boolean get() = loadState.isLoading(BookmarkedShopLoadKey.FETCH)

    val isOnlyLoading: Boolean get() = shops.isEmpty() && (!hasLoaded || isPageLoading)

    val isOverlayLoading =
        loadState.isLoading(BookmarkedShopLoadKey.REMOVE) ||
            loadState.isLoading(BookmarkedShopLoadKey.PERSONALIZATION)
}
