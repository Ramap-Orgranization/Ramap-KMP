package com.peto.ramap.domain.model.review

data class MyReviewsPage(
    val reviews: List<MyReview>,
    val totalCount: Int,
    val publicCount: Int,
    val privateCount: Int,
    val profileIsPublic: Boolean,
)
