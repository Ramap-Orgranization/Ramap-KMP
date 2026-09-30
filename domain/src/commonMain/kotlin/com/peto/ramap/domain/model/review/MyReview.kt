package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus

data class MyReview(
    val id: String,
    val shopId: String,
    val shopName: String,
    val body: String,
    val createdAt: String,
    val images: List<MyReviewPhoto>,
    val isPublic: Boolean,
    val moderationStatus: ReviewModerationStatus,
    val moderationNote: String? = null,
    val authorAvatarUrl: String? = null,
) {
    fun toReview(): Review =
        Review(
            id = id,
            shopId = shopId,
            body = body,
            createdAt = createdAt,
            imageUrls = images.mapNotNull { it.url },
            author =
                ReviewAuthor(
                    userId = "",
                    nickname = shopName,
                    avatarUrl = authorAvatarUrl,
                ),
            isPublic = isPublic,
            moderationStatus = moderationStatus,
        )
}
