package com.peto.ramap.domain.model.review.change

data class ReviewOwnerCounts(
    val totalCount: Int,
    val publicCount: Int,
    val privateCount: Int,
    val publishedCount: Int,
    val profileIsPublic: Boolean,
)
