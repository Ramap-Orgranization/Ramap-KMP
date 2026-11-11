package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewCommentCountResponse(
    @SerialName("review_id") val reviewId: String,
    @SerialName("comment_count") val commentCount: Int,
)
