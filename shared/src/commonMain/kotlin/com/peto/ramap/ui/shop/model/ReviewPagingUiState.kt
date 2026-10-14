package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable

@Immutable
data class ReviewPagingUiState(
    val isRetryingReviews: Boolean = false,
    val showReviewsOnOpen: Boolean = false,
    val hasMoreReviews: Boolean = false,
    val isLoadingMoreReviews: Boolean = false,
)
