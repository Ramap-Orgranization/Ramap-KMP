package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ReviewPagingUiState
import com.peto.ramap.ui.shop.model.UserContext
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.contract.ShopReviewUiState

@Immutable
data class ShopDetailContentUiState(
    val sheetState: ShopOverviewUiState,
    val headerState: HeaderUiState,
    val reviewPagingState: ReviewPagingUiState,
    val userContext: UserContext,
    val isLoggedIn: Boolean,
    val menuState: ShopMenuUiState? = null,
    val reviewState: ShopReviewUiState? = null,
    val isShopUnavailable: Boolean = false,
    val overviewHasLoadFailure: Boolean = false,
    val overviewIsLoading: Boolean = false,
)
