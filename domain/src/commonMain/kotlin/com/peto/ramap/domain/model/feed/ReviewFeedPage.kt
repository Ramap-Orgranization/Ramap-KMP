package com.peto.ramap.domain.model.feed

import com.peto.ramap.domain.model.review.ProfileReview

data class ReviewFeedPage(
    val reviews: List<ProfileReview> = emptyList(),
    val nextCursor: ReviewFeedCursor? = null,
)
