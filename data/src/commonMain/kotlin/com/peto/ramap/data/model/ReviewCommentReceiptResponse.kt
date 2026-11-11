package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.comment.ReviewCommentReceipt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewCommentReceiptResponse(
    val comment: ReviewCommentResponse,
    @SerialName("comment_count") val commentCount: Int,
) {
    fun toDomain(): ReviewCommentReceipt = ReviewCommentReceipt(comment.toComment(), commentCount)
}
