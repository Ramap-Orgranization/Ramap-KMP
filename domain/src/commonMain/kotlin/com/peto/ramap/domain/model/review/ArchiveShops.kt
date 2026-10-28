package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.shop.RamenShops

data class ArchiveShops(
    val reviews: List<MyReview>,
    val shops: RamenShops,
) {
    fun reviewsForShop(shopId: String): List<MyReview> =
        reviews
            .filter { it.review.shopId == shopId && shopId in shops }
            .sortedWith(compareByDescending<MyReview> { it.review.createdAt }.thenByDescending { it.review.id })

    fun representativePhotos(): Map<String, MyReviewPhoto> =
        reviews
            .sortedWith(compareByDescending<MyReview> { it.review.createdAt }.thenByDescending { it.review.id })
            .filter { it.review.images.isNotEmpty() && it.review.shopId in shops }
            .distinctBy { it.review.shopId }
            .associate { it.review.shopId to it.review.images.first() }
}
