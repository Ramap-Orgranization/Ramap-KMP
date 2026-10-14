package com.peto.ramap.domain.model.review

data class ShopReviewsPage(
    val reviews: List<Review>,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = reviews.size < totalCount
}
