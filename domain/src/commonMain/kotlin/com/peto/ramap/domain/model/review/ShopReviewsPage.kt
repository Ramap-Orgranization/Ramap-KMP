package com.peto.ramap.domain.model.review

data class ShopReviewsPage(
    val reviews: List<Review>,
    val totalCount: Int,
)
