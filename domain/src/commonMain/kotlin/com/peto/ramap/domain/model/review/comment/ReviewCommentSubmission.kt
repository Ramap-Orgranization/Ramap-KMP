package com.peto.ramap.domain.model.review.comment

data class ReviewCommentSubmission(
    val reviewId: String,
    val parentId: String?,
    val body: String,
    val requestId: String,
    val expectedAuthorId: String,
)
