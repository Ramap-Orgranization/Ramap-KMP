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
            author = ReviewAuthor(userId, nickname, avatarUrl),
            moderationStatus = ReviewModerationStatus.entries.find { it.name.equals(moderationStatus, ignoreCase = true) } ?: ReviewModerationStatus.PENDING,
            isPublic = isPublic,
        )
}
