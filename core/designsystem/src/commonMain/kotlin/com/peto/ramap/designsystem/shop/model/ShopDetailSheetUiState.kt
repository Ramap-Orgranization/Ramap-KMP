package com.peto.ramap.designsystem.shop.model

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail

sealed interface ShopDetailSheetUiState {
    data object Closed : ShopDetailSheetUiState

    data class Loading(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailSheetUiState

    data class Content(
        val detail: ShopDetail,
    ) : ShopDetailSheetUiState

    data class Error(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailSheetUiState
}
