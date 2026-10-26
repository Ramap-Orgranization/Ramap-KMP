package com.peto.ramap.domain.model.feed

data class ReviewFeedCursor(
    val createdAt: String,
    val reviewId: String,
    val likeCount: Int? = null,
)
