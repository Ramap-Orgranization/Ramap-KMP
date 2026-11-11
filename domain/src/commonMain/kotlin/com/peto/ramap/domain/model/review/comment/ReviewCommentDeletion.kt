package com.peto.ramap.domain.model.review.comment

data class ReviewCommentDeletion(
    val commentCount: Int,
    val keepThread: Boolean,
)
