package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class ReviewResponse(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("image_paths") val imagePaths: List<String> = emptyList(),
    @SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("moderation_status") val moderationStatus: String = "pending",
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("visit_number") val visitNumber: Int = 1,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("author_review_count") val authorReviewCount: Int = 0,
    @SerialName("is_blocked") val isBlocked: Boolean = false,
    @Transient val imageUrls: List<String> = emptyList(),
    @Transient val avatarUrl: String? = null,
) {
    fun toDomain(): Review =
        Review(
            id = id,
            shopId = shopId,
            body = body,
            createdAt = createdAt,
            imageUrls = imageUrls,
            author = ReviewAuthor(userId, nickname, avatarUrl, authorReviewCount),
            moderationStatus = ReviewModerationStatus.entries.find { it.name.equals(moderationStatus, ignoreCase = true) } ?: ReviewModerationStatus.PENDING,
            isPublic = isPublic,
            visitNumber = visitNumber,
            likeCount = likeCount,
            isLiked = isLiked,
            isBlocked = isBlocked,
        )
}
