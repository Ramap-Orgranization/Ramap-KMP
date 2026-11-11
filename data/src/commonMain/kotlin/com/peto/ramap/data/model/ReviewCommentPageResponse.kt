package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.comment.ReviewCommentPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewCommentPageResponse(
    val comments: List<ReviewCommentResponse>,
    @SerialName("has_more") val hasMore: Boolean,
    @SerialName("comment_count") val commentCount: Int,
) {
    fun toDomain(): ReviewCommentPage =
        ReviewCommentPage(
            threads = comments.map(ReviewCommentResponse::toThread),
            nextCursor =
                comments
                    .lastOrNull()
                    ?.toComment()
                    ?.cursor
                    ?.takeIf { hasMore },
            commentCount = commentCount,
        )
}
