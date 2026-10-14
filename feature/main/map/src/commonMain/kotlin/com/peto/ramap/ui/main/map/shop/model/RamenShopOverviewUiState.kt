package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.usecase.ShopDetail

@Immutable
data class RamenShopOverviewUiState(
    val detail: ShopDetail,
    val headerState: ShopOverviewHeaderUiState,
    val menu: ShopMenu? = null,
    val isMenuLoading: Boolean = false,
    val hasMenuLoadFailure: Boolean = false,
    val reviewPagingState: ShopOverviewReviewPagingUiState = ShopOverviewReviewPagingUiState(),
    val userContext: ShopOverviewUserContext = ShopOverviewUserContext(),
)
