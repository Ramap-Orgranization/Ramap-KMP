package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class MyReviewResponse(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("avatar_path") val authorAvatarPath: String? = null,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("image_paths") val imagePaths: List<String>,
    @SerialName("moderation_status") val moderationStatus: String,
    @SerialName("moderation_note") val moderationNote: String? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @Transient val imageUrls: List<String?> = emptyList(),
    @Transient val authorAvatarUrl: String? = null,
) {
    fun toDomain(): MyReview =
        MyReview(
            id = id,
            shopId = shopId,
            shopName = shopName,
            body = body,
            createdAt = createdAt,
            images = imagePaths.mapIndexed { index, path -> MyReviewPhoto(imageUrls.getOrNull(index), path) },
            isPublic = isPublic,
            moderationStatus =
                ReviewModerationStatus.entries.find { it.name.equals(moderationStatus, ignoreCase = true) }
                    ?: ReviewModerationStatus.PENDING,
            moderationNote = moderationNote,
            authorAvatarUrl = authorAvatarUrl,
        )
}
