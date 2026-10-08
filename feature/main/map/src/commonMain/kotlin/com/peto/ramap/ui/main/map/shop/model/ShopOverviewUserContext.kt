package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.review.Review

@Immutable
data class ShopOverviewUserContext(
    val currentUserId: String? = null,
    val currentProfileIsPublic: Boolean? = null,
    val actingReviewId: String? = null,
    val revealedBlockedReviews: Map<String, Review> = emptyMap(),
    val revealingBlockedReviewId: String? = null,
)
