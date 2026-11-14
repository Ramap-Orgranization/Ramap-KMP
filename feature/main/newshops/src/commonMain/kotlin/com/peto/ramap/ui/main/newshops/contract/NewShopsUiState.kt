package com.peto.ramap.ui.main.newshops.contract

import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class NewShopsUiState(
    val month: ShopOpeningMonth,
    val shops: List<NewShop>? = null,
    val failed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<NewShopsUiState> {
    val isLoading: Boolean get() = loadState.isLoading(NewShopsLoadKey.Fetch)
    val isRefreshing: Boolean get() = loadState.isLoading(NewShopsLoadKey.Refresh)
    val totalShopCount: Int get() = shops.orEmpty().size

    override fun withLoadingState(loadState: LoadState): NewShopsUiState = copy(loadState = loadState)

    fun visibleIn(currentMonth: ShopOpeningMonth): NewShopsUiState = if (currentMonth == month) this else NewShopsUiState(month = currentMonth)
}
