package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.review.Review

@Immutable
data class ShopDetailReviewActions(
    val onReviewsClick: (String) -> Unit,
    val onViewBlockedReview: ((Review) -> Unit)? = null,
    val onUnblockBlockedUser: ((Review) -> Unit)? = null,
    val onOpenProfile: (String) -> Unit = {},
    val onReviewLike: (Review) -> Unit = {},
    val onReviewEdit: (Review) -> Unit = {},
    val onReviewDelete: (Review) -> Unit = {},
    val onReviewReport: (Review) -> Unit = {},
    val onLoadMoreReviews: () -> Unit = {},
)
