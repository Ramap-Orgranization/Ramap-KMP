package com.peto.ramap.data.model

import com.peto.ramap.domain.repository.ReviewLike
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewLikeResponse(
    @SerialName("like_count")
    val likeCount: Int,
    @SerialName("is_liked")
    val isLiked: Boolean,
) {
    fun toDomain(): ReviewLike = ReviewLike(likeCount, isLiked)
}
