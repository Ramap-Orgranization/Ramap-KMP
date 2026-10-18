package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class SubscribedShopsUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val subscribedEvents: List<ShopEvent> = emptyList(),
    val showShopError: Boolean = false,
    val showEventError: Boolean = false,
    val haveShopsLoaded: Boolean = false,
    val haveEventsLoaded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<SubscribedShopsUiState> {
    override fun withLoadingState(loadState: LoadState): SubscribedShopsUiState = copy(loadState = loadState)

    val showError: Boolean = showShopError || showEventError

    val isOnlyLoading: Boolean =
        shops.isEmpty() &&
            subscribedEvents.isEmpty() &&
            (
                !haveShopsLoaded ||
                    !haveEventsLoaded ||
                    loadState.isLoading(SubscribedShopsLoadKey.SHOPS) ||
                    loadState.isLoading(SubscribedShopsLoadKey.EVENTS)
            )

    val isOverlayLoading =
        loadState.isLoading(SubscribedShopsLoadKey.REMOVE) ||
            loadState.isLoading(SubscribedShopsLoadKey.SHOPS) ||
            loadState.isLoading(SubscribedShopsLoadKey.EVENTS)

    val showsEmptyContent: Boolean get() = shops.isEmpty() && subscribedEvents.isEmpty()
    val showsSubscribedEvents: Boolean get() = subscribedEvents.isNotEmpty()
    val showsShops: Boolean get() = shops.isNotEmpty()
}
