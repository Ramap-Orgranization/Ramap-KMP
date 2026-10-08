package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.usecase.ShopDetail

@Immutable
data class RamenShopOverviewUiState(
    val detail: ShopDetail,
    val headerState: ShopOverviewHeaderUiState,
    val reviewPagingState: ShopOverviewReviewPagingUiState = ShopOverviewReviewPagingUiState(),
    val userContext: ShopOverviewUserContext = ShopOverviewUserContext(),
)
