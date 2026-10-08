package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.review.Review

@Immutable
data class ShopOverviewReviewActions(
    val onOpenProfile: (String) -> Unit,
    val onWriteReviewClick: () -> Unit,
    val onReviewRetry: () -> Unit = {},
    val onViewBlockedReview: ((Review) -> Unit)? = null,
    val onUnblockBlockedUser: ((Review) -> Unit)? = null,
    val onReviewLike: (Review) -> Unit = {},
    val onReviewEdit: (Review) -> Unit = {},
    val onReviewDelete: (Review) -> Unit = {},
    val onReviewReport: (Review) -> Unit = {},
    val onLoadMoreReviews: () -> Unit = {},
)
