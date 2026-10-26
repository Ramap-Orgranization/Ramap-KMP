package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewModerationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

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
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("moderation_note") val moderationNote: String? = null,
    @Transient val imageReferences: List<MyReviewPhoto> = emptyList(),
    @Transient val imageUrls: List<String> = emptyList(),
    @Transient val avatarUrl: String? = null,
    @Transient val avatarExpiresAt: Instant? = null,
) {
    fun toDomain(): Review =
        Review(
            id = id,
            shopId = shopId,
            body = body,
            createdAt = createdAt,
            imageUrls = imageUrls,
            images = imageReferences.ifEmpty { imagePaths.map { MyReviewPhoto(null, it) } },
            author = ReviewAuthor(userId, nickname, avatarUrl, authorReviewCount, avatarPath, avatarExpiresAt),
            moderationStatus = ReviewModerationStatus.entries.find { it.name.equals(moderationStatus, ignoreCase = true) } ?: ReviewModerationStatus.PENDING,
            isPublic = isPublic,
            visitNumber = visitNumber,
            likeCount = likeCount,
            isLiked = isLiked,
            isBlocked = isBlocked,
        )
}
