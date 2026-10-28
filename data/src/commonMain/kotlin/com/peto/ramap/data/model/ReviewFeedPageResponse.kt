package com.peto.ramap.data.model

import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewFeedPageResponse(
    val reviews: List<ProfileReviewResponse> = emptyList(),
    @SerialName("next_created_at") val nextCreatedAt: String? = null,
    @SerialName("next_review_id") val nextReviewId: String? = null,
    @SerialName("next_like_count") val nextLikeCount: Int? = null,
    @SerialName("has_filter_targets") val hasFilterTargets: Boolean? = null,
) {
    fun nextCursor(): ReviewFeedCursor? {
        val createdAt = nextCreatedAt ?: return null
        val reviewId = nextReviewId ?: return null
        return ReviewFeedCursor(createdAt, reviewId, nextLikeCount)
    }
}
