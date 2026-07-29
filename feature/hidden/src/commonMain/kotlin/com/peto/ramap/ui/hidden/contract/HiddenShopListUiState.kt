package com.peto.ramap.ui.hidden.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class HiddenShopListUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val showError: Boolean = false,
    val hasLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<HiddenShopListUiState> {
    override fun withLoadingState(loadState: LoadState): HiddenShopListUiState = copy(loadState = loadState)

    val isOnlyLoading: Boolean =
        shops.isEmpty() &&
            (!hasLoaded || loadState.isLoading(HiddenShopLoadKey.FETCH))

    val isOverlayLoading: Boolean =
        loadState.isLoading(HiddenShopLoadKey.UNHIDE) ||
            loadState.isLoading(HiddenShopLoadKey.FETCH)
}
