package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.CommunityFeedCursor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CommunityFeedPageResponse(
    val reviews: List<ProfileReviewResponse> = emptyList(),
    @SerialName("next_created_at") val nextCreatedAt: String? = null,
    @SerialName("next_review_id") val nextReviewId: String? = null,
) {
    fun nextCursor(): CommunityFeedCursor? {
        val createdAt = nextCreatedAt ?: return null
        val reviewId = nextReviewId ?: return null
        return CommunityFeedCursor(createdAt, reviewId)
    }
}
