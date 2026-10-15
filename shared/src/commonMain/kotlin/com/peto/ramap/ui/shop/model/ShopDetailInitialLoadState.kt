package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState

@Immutable
data class ShopDetailInitialLoadState(
    val shopId: String,
    private val overviewCompleted: Boolean = false,
    private val menuCompleted: Boolean = false,
    private val reviewsCompleted: Boolean = false,
    private val hasPresentedOverview: Boolean = false,
) {
    private val isComplete: Boolean get() = overviewCompleted && menuCompleted && reviewsCompleted

    fun update(
        overview: ShopOverviewState,
        menu: ShopMenuUiState,
        reviews: ReviewsUiState,
    ): ShopDetailInitialLoadState {
        val next =
            copy(
                overviewCompleted = overviewCompleted || (matchesOverview(overview) && !overview.isLoading && (overview.shopOverview != null || overview.hasLoadFailure)),
                menuCompleted = menuCompleted || (menu.shopId == shopId && !menu.isLoading && (menu.menu != null || menu.hasLoadFailure)),
                reviewsCompleted = reviewsCompleted || (reviews.shopId == shopId && !reviews.isLoading && (reviews.hasLoaded || reviews.hasReviewLoadFailure)),
            )
        return next.copy(
            hasPresentedOverview = hasPresentedOverview || (next.isComplete && matchesOverview(overview) && !overview.isLoading && !overview.hasLoadFailure && overview.shopOverview != null),
        )
    }

    fun displayState(overview: ShopOverviewState): ShopDetailDisplayState =
        when {
            !isComplete -> ShopDetailDisplayState.InitialSkeleton
            !matchesOverview(overview) -> ShopDetailDisplayState.OverviewLoading
            hasPresentedOverview && overview.shopOverview != null -> ShopDetailDisplayState.Content
            overview.isLoading -> ShopDetailDisplayState.OverviewLoading
            else -> ShopDetailDisplayState.OverviewError
        }

    private fun matchesOverview(overview: ShopOverviewState): Boolean =
        when (val state = overview.overviewState) {
            is ShopOverviewUiState.Content -> state.overview.shop.id == shopId
            is ShopOverviewUiState.Loading -> state.shopId == shopId
            is ShopOverviewUiState.Error -> state.shopId == shopId
            ShopOverviewUiState.Closed -> false
        }
}
