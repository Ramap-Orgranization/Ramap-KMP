package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.comment.ReviewCommentDeletion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewCommentDeletionResponse(
    @SerialName("comment_count") val commentCount: Int,
    @SerialName("keep_thread") val keepThread: Boolean,
) {
    fun toDomain(): ReviewCommentDeletion = ReviewCommentDeletion(commentCount, keepThread)
}
