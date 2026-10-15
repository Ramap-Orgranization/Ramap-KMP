package com.peto.ramap.ui.shop.review.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.review.Review

@Immutable
data class ReviewActions(
    val onWriteReviewClick: () -> Unit,
    val onReviewLike: (Review) -> Unit,
    val onRetry: () -> Unit,
    val onOpenProfile: (String) -> Unit,
    val onReviewEdit: (Review) -> Unit,
    val onReviewDelete: (Review) -> Unit,
    val onReviewReport: (Review) -> Unit,
    val onLoadMoreReviews: () -> Unit,
    val onViewBlockedReview: ((Review) -> Unit)? = null,
    val onUnblockBlockedUser: ((Review) -> Unit)? = null,
)
