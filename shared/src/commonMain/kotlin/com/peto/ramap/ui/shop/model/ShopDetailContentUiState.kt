package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState

@Immutable
data class ShopDetailContentUiState(
    val shopId: String,
    val displayState: ShopDetailDisplayState,
    val sheetState: ShopOverviewUiState,
    val headerState: HeaderUiState,
    val isLoggedIn: Boolean,
    val menuState: ShopMenuUiState? = null,
    val reviewState: ReviewsUiState = ReviewsUiState(),
    val overviewHasLoadFailure: Boolean = false,
)
