package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable

@Immutable
data class ShopOverviewReviewPagingUiState(
    val isRetryingReviews: Boolean = false,
    val showReviewsOnOpen: Boolean = false,
    val hasMoreReviews: Boolean = false,
    val isLoadingMoreReviews: Boolean = false,
)
