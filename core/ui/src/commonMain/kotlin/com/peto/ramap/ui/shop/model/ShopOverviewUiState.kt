package com.peto.ramap.ui.shop.model

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopOverView

sealed interface ShopOverviewUiState {
    data object Closed : ShopOverviewUiState

    data class Loading(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopOverviewUiState

    data class Content(
        val overview: ShopOverView,
    ) : ShopOverviewUiState

    data class Error(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopOverviewUiState
}
