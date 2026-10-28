package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewModerationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class MyReviewResponse(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("avatar_path") val authorAvatarPath: String? = null,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("image_paths") val imagePaths: List<String>,
    @SerialName("moderation_status") val moderationStatus: String,
    @SerialName("moderation_note") val moderationNote: String? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("visit_number") val visitNumber: Int = 1,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("author_review_count") val authorReviewCount: Int = 0,
    @Transient val imageReferences: List<MyReviewPhoto> = emptyList(),
    @Transient val imageUrls: List<String?> = emptyList(),
    @Transient val authorAvatarUrl: String? = null,
    @Transient val authorAvatarExpiresAt: kotlin.time.Instant? = null,
) {
    fun toDomain(): MyReview {
        val images = imageReferences.ifEmpty { imagePaths.mapIndexed { index, path -> MyReviewPhoto(imageUrls.getOrNull(index), path) } }
        return MyReview(
            review =
                Review(
                    id = id,
                    shopId = shopId,
                    body = body,
                    createdAt = createdAt,
                    imageUrls = images.mapNotNull { it.url },
                    images = images,
                    isPublic = isPublic,
                    moderationStatus =
                        ReviewModerationStatus.entries.find { it.name.equals(moderationStatus, ignoreCase = true) }
                            ?: ReviewModerationStatus.PENDING,
                    visitNumber = visitNumber,
                    likeCount = likeCount,
                    author = ReviewAuthor(userId, nickname, authorAvatarUrl, authorReviewCount, authorAvatarPath, authorAvatarExpiresAt),
                ),
            shopName = shopName,
            moderationNote = moderationNote,
        )
    }
}
