package com.peto.ramap.ui.shop.overview.contract

import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.shop.model.ShopDetailSheetUiState

data class ShopOverviewState(
    val shopDetailState: ShopDetailSheetUiState = ShopDetailSheetUiState.Closed,
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
        when (val sheet = shopDetailState) {
            is ShopDetailSheetUiState.Content -> sheet.detail.shop
            is ShopDetailSheetUiState.Loading -> sheet.shop
            is ShopDetailSheetUiState.Error -> sheet.shop
            ShopDetailSheetUiState.Closed -> null
        }
    val shopDetail: ShopDetail? get() = (shopDetailState as? ShopDetailSheetUiState.Content)?.detail
    val isLoading: Boolean get() = loadState.isLoading(ShopOverviewLoadKey.Overview)

    override fun withLoadingState(loadState: LoadState): ShopOverviewState = copy(loadState = loadState)
}
