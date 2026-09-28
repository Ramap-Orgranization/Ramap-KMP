package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.review.ReviewImage

internal interface ReviewDataSource {
    suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun submitReview(
        review: ShopReviewRequest,
        images: List<ReviewImage>,
    )
}
