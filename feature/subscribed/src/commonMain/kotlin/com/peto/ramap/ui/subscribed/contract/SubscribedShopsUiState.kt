package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class SubscribedShopsUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val showError: Boolean = false,
    val haveShopsLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<SubscribedShopsUiState> {
    override fun withLoadingState(loadState: LoadState): SubscribedShopsUiState = copy(loadState = loadState)

    val isOnlyLoading: Boolean get() = shops.isEmpty() && (!haveShopsLoaded || isOverlayLoading)

    val isOverlayLoading: Boolean get() = loadState.isLoading(SubscribedShopsLoadKey.SHOPS)
}
