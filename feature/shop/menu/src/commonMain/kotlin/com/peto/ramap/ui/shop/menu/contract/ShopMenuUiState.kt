package com.peto.ramap.ui.shop.menu.contract

import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ShopMenuUiState(
    val shopId: String? = null,
    val menu: ShopMenu? = null,
    val hasLoadFailure: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ShopMenuUiState> {
    val isLoading: Boolean get() = loadState.isLoading(ShopMenuLoadKey.Menu)

    override fun withLoadingState(loadState: LoadState): ShopMenuUiState = copy(loadState = loadState)
}
