package com.peto.ramap.ui.shop.overview.contract

import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.shop.model.ShopOverviewUiState

data class ShopOverviewState(
    val overviewState: ShopOverviewUiState = ShopOverviewUiState.Closed,
    val shopWaiting: Map<String, WaitingSystem?> = emptyMap(),
    val entryGeneration: Long = 0,
    val isLoggedIn: Boolean = false,
    val personalization: ShopPersonalization = ShopPersonalization(),
    val hasLoadFailure: Boolean = false,
    val isShopUnavailable: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ShopOverviewState> {
    val bookmarkedShopIds: Set<String> get() = personalization.bookmarkedShopIds
    val hiddenShopIds: Set<String> get() = personalization.hiddenShopIds
    val notificationShopIds: Set<String> get() = personalization.notificationShopIds
    val selectedShop: RamenShop? get() =
        when (val sheet = overviewState) {
            is ShopOverviewUiState.Content -> sheet.overview.shop
            is ShopOverviewUiState.Loading -> sheet.shop
            is ShopOverviewUiState.Error -> sheet.shop
            ShopOverviewUiState.Closed -> null
        }
    val shopOverview: ShopOverView? get() = (overviewState as? ShopOverviewUiState.Content)?.overview
    val isLoading: Boolean get() = loadState.isLoading(ShopOverviewLoadKey.Overview)

    override fun withLoadingState(loadState: LoadState): ShopOverviewState = copy(loadState = loadState)
}
