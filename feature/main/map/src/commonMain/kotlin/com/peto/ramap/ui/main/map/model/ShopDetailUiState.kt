package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail

sealed interface ShopDetailUiState {
    data object Closed : ShopDetailUiState

    data class Loading(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailUiState

    data class Content(
        val detail: ShopDetail,
    ) : ShopDetailUiState

    data class Error(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailUiState
}
