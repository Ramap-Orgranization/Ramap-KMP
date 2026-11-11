package com.peto.ramap.domain.model.review.comment

data class ReviewCommentPage(
    val threads: List<ReviewCommentThread>,
    val nextCursor: ReviewCommentCursor?,
    val commentCount: Int,
)
