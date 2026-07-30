package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class SubscribedShopListUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val subscribedEvents: List<ShopEvent> = emptyList(),
    val showShopError: Boolean = false,
    val showEventError: Boolean = false,
    val haveShopsLoaded: Boolean = false,
    val haveEventsLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<SubscribedShopListUiState> {
    override fun withLoadingState(loadState: LoadState): SubscribedShopListUiState = copy(loadState = loadState)

    val showError: Boolean = showShopError || showEventError

    val isOnlyLoading: Boolean =
        shops.isEmpty() &&
            subscribedEvents.isEmpty() &&
            (
                !haveShopsLoaded ||
                    !haveEventsLoaded ||
                    loadState.isLoading(SubscribedShopLoadKey.SHOPS) ||
                    loadState.isLoading(SubscribedShopLoadKey.EVENTS)
            )

    val isOverlayLoading =
        loadState.isLoading(SubscribedShopLoadKey.REMOVE) ||
            loadState.isLoading(SubscribedShopLoadKey.SHOPS) ||
            loadState.isLoading(SubscribedShopLoadKey.EVENTS)
}
