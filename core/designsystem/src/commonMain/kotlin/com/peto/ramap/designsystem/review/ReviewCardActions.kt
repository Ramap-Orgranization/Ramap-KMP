package com.peto.ramap.designsystem.review

data class ReviewCardActions(
    val onOpenProfile: ((String) -> Unit)? = null,
    val onShopClick: (() -> Unit)? = null,
    val onLike: (() -> Unit)? = null,
    val onReport: (() -> Unit)? = null,
    val onEdit: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
    val onViewBlockedReview: (() -> Unit)? = null,
    val onUnblockBlockedUser: (() -> Unit)? = null,
    val onReviewClick: (() -> Unit)? = null,
)
