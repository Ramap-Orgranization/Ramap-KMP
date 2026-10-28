package com.peto.ramap.domain.model.review.change

import com.peto.ramap.domain.model.follow.FollowAction
import com.peto.ramap.domain.model.review.ReviewLike

sealed interface ReviewChange {
    data class Written(
        val reviewId: String,
        val userId: String,
        val shopId: String,
        val before: ReviewSnapshot?,
        val after: ReviewSnapshot?,
        val counts: ReviewOwnerCounts,
    ) : ReviewChange

    data class Hydrated(
        val snapshot: ReviewSnapshot,
    ) : ReviewChange

    data class Liked(
        val reviewId: String,
        val result: ReviewLike,
    ) : ReviewChange

    data class AccessChanged(
        val userId: String,
        val followAction: FollowAction? = null,
    ) : ReviewChange
}
