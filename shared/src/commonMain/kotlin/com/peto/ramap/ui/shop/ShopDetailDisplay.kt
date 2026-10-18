package com.peto.ramap.ui.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ShopDetailDisplayState
import com.peto.ramap.ui.shop.model.ShopDetailInitialLoadState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState

@Composable
internal fun rememberShopDetailDisplayState(
    shopId: String,
    overview: ShopOverviewState,
    menu: ShopMenuUiState,
    reviews: ReviewsUiState,
): ShopDetailDisplayState {
    var initialLoad by remember(shopId) { mutableStateOf(ShopDetailInitialLoadState(shopId)) }
    val next = initialLoad.update(overview, menu, reviews)
    SideEffect { initialLoad = next }
    return next.displayState(overview)
}
